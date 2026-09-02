# Module Guide

The procedure for adding a feature to the Asset Tagging System, and the conventions all
new code must follow. [docs/ARCHITECTURE.md](ARCHITECTURE.md) describes the layers this
guide operates within.

The worked example throughout is an **Approvals** module: an administrator queue and a
decision page. The same nine steps apply to any new feature.

## 1. Define the read models

A service returns records shaped for the page, never JPA entities. Create them in `dto/`.

```java
package com.sil.asset_tagging_system.dto;

import java.time.LocalDateTime;

import com.sil.asset_tagging_system.model.enums.ApprovalStatus;
import com.sil.asset_tagging_system.model.enums.RequestType;

public record ApprovalRow(
        Long id,
        String assetTag,
        String requesterName,
        RequestType requestType,
        ApprovalStatus status,
        LocalDateTime requestedAt
) {}
```

Display values such as `assetTag` and `requesterName` are joined in SQL and arrive already
resolved. A page that renders `#{row.assetTag}` cannot trigger a query, because there is
nothing lazy left to load.

Records are the correct shape here: immutable, no Lombok required, and their accessors are
reached from expression language as `#{row.assetTag}`.

## 2. Define the command

Input travels in the opposite direction as a record in `dto/command/`.

```java
package com.sil.asset_tagging_system.dto.command;

import com.sil.asset_tagging_system.model.enums.ApprovalActionType;

public record DecideApprovalCommand(
        Long approvalId,
        ApprovalActionType action,
        String notes
) {}
```

## 3. Add DAO methods

In `dao/`. One SQL statement per method, no business rules, and **no** `@Transactional`.

```java
public List<ApprovalRow> findQueue(ApprovalStatus status, int limit, int offset)
public long countQueue(ApprovalStatus status)
public Optional<ApprovalDetail> findDetail(Long id)
public void insertAction(Long approvalId, Long actorUserId, ApprovalActionType action, int sequenceNo, String notes)
public void updateStatus(Long id, ApprovalStatus status)
```

Use `EntityManager.createNativeQuery(...)` and map `Object[]` rows into the record by hand.
Pagination belongs in SQL via `LIMIT` and `OFFSET`, with a matching `COUNT(*)` method —
never by loading every row and slicing the list in Java.

`DaoUtils.exists(...)` and `DaoUtils.getLastInsertId(...)` are available for existence
checks and generated keys.

## 4. Write the service

`service/ApprovalService.java`. This is where rules live and where transactions begin.

```java
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalDao approvalDao;
    private final AuditTrail auditTrail;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void decide(DecideApprovalCommand command, Actor actor) {

        // 1. Load current state and validate the transition is legal
        // 2. Insert the approval_actions row
        // 3. Recompute and update the approval status
        // 4. Write the activity_log row -- in this same transaction
    }
}
```

Five requirements, each with a reason:

| Requirement | Reason |
|---|---|
| `@Transactional` on this method | The whole action is atomic. A failure at step 4 rolls back steps 2 and 3 |
| The activity-log write is inside it | The governing audit rule: if the log write fails, the action fails |
| Rows are written through `AuditTrail`, never `ActivityLogDao` directly | It owns the correlation id, the default sequence, the outcome, the role parse, and — the part that matters — the choice between the transactional and the `REQUIRES_NEW` write path. A call site that makes that choice by hand can get it backwards, and getting it backwards silently loses the row |
| The actor arrives as one `Actor` parameter | `(userId, role, ipAddress)` always travel together. Passed separately they had already drifted into two different orderings of the same `(Long, String, String)` triple across two services — an argument swap the compiler cannot catch |
| No `jakarta.faces` import | Keeps the service callable from a job, a test, or an event listener. This is also why the service receives an `Actor` rather than calling `Actor.current()` itself |
| Validation before mutation | Business rules belong here, not in the bean and not in the DAO |

A service that both mutates and refuses reads like this. Note that `refused(...)` is what
selects the `REQUIRES_NEW` path — the call site states intent, not plumbing:

```java
if (approvalDao.existsOpenTransferRequest(assetId)) {
    auditTrail.record(REQUEST_SUBMITTED, APPROVAL)
            .by(actor)
            .asset(assetId)
            .refused("A transfer is already pending for this asset")   // DENIED + REQUIRES_NEW
            .summary("Transfer request refused -- one is already pending for asset " + assetId)
            .save();
    throw new BusinessRuleException("A transfer is already pending for this asset");
}

// ... perform the mutation ...

auditTrail.record(REQUEST_SUBMITTED, APPROVAL)   // outcome defaults to SUCCEEDED
        .by(actor)
        .asset(assetId)
        .approval(approvalId)
        .holder(previousHolderId, requesterId)
        .summary("Transfer requested for asset " + assetId)
        .save();                                  // joins this transaction
```

Populate the columns the action owns rather than only `summary`: a condition change sets
`.condition(previous, next)`, a custody move sets `.holder(previous, next)`, and an action
producing more than one row sets `.sequence(2)` on the second under the shared correlation
id. Those columns are what make the audit screen answerable; a row carrying only a prose
summary cannot be filtered or compared.

The authentication paths are the exception to all of the above. They have no business
transaction to join and must never break a sign-in, so they add `.bestEffort()`, which
swallows a logging failure rather than propagating it.

## 5. Write the backing beans

In `bean/approval/`. Scope is determined by whether the view carries a form.

A read-only queue is `@RequestScoped`:

```java
package com.sil.asset_tagging_system.bean.approval;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import lombok.Getter;

@Named
@RequestScoped
@Getter
public class ApprovalQueueView {

    @Inject
    private ApprovalService approvalService;

    private List<ApprovalRow> rows;
    private Integer currentPage;

    public void load() {
        rows = approvalService.findQueue(currentPage, PAGE_SIZE);
    }
}
```

A decision page carries a form and is therefore `@ViewScoped`:

```java
import java.io.Serializable;

import jakarta.faces.view.ViewScoped;   // NOT jakarta.faces.bean.ViewScoped
import jakarta.inject.Named;

@Named
@ViewScoped
public class ApprovalDecisionView implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private transient ApprovalService approvalService;
}
```

A postback is a new HTTP request, so a `@RequestScoped` bean would be a fresh instance with
every field reset — state cannot survive a submission. `@ViewScoped` beans must implement
`Serializable`, declare a `serialVersionUID`, and mark injected services `transient`, since
those are Spring singletons that must not be serialized with the bean.

Note that `jakarta.faces.bean.ViewScoped` was removed in Jakarta Faces 4.0. Only
`jakarta.faces.view.ViewScoped` works with `@Named`.

The bean calls the service and returns a navigation outcome. It contains no business rules
and never touches a DAO.

## 6. Create the views

Create the directory `webapp/approval/`. **The directory name becomes the URL segment.**

| File | URL |
|---|---|
| `webapp/approval/queue.xhtml` | `/approval/queue` |
| `webapp/approval/detail.xhtml` | `/approval/detail?id=7` |

Each page templates from `main.xhtml` and defines only its own content. It declares no
`<html>`, no `<h:head>`, and no stylesheet links — the template owns the document.

```xml
<ui:composition template="/WEB-INF/templates/main.xhtml"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:h="jakarta.faces.html"
                xmlns:f="jakarta.faces.core"
                xmlns:ui="jakarta.faces.facelets"
                xmlns:ats="jakarta.faces.composite/ats">

    <ui:define name="title">Approval queue</ui:define>

    <ui:define name="content">
        <ats:pageHeader title="Approvals" subtitle="Pending requests"/>

        <h:dataTable value="#{approvalQueueView.rows}" var="row" styleClass="table">
            <h:column>
                <f:facet name="header">Asset</f:facet>
                <h:link outcome="/approval/detail" value="#{row.assetTag}">
                    <f:param name="id" value="#{row.id}"/>
                </h:link>
            </h:column>
        </h:dataTable>

        <ats:pagination currentPage="#{approvalQueueView.currentPage}"
                        totalPages="#{approvalQueueView.totalPages}"
                        outcome="/approval/queue"/>
    </ui:define>
</ui:composition>
```

Anything placed outside `ui:composition` is discarded — that is what makes the mechanism
safe.

## 7. Declare URL parameters

Values arriving in the URL are declared, never read from the request parameter map.

```xml
<f:metadata>
    <f:viewParam name="id" value="#{approvalDecisionView.id}" required="true"
                 requiredMessage="An approval id is required."/>
    <f:viewAction action="#{approvalDecisionView.load}"/>
</f:metadata>
```

`f:viewParam` converts `String` to `Long`, enforces required-ness, and turns `?id=abc` into
a message on the page rather than an exception thrown out of a lifecycle method. Because it
writes through a value expression, the bound property requires a **setter**.

`f:viewAction` runs after parameters are bound and before rendering. `@PostConstruct` runs
*before* binding and therefore cannot see them, which is why loading logic belongs in a
`f:viewAction` method.

## 8. Add navigation and security

A link in `WEB-INF/fragments/sidebar.xhtml`:

```xml
<h:link outcome="/approval/queue" value="Approvals" styleClass="nav-link"/>
```

`h:link outcome=` resolves the view id and prepends the context path automatically. Never
write a literal `href` — it will omit the context path and break outside a root deployment.

One rule in `SecurityConfig`:

```java
.requestMatchers("/approval/**").hasRole("ADMIN")
```

One rule per route. The `.xhtml` form of the URL does not exist, so it needs no companion
matcher.

Rules that depend on data rather than URL — an administrator may not approve a request they
raised themselves — are enforced as an explicit check inside the service method, not
`@PreAuthorize`: see `ApprovalService.recordAction` for the real example. Back it with a
database constraint where one is possible (here, `approval_actions`' `UNIQUE(approval_id,
actor_user_id)`), so the rule holds even if the application-level check is ever bypassed or
mistakenly removed. `@PreAuthorize` with a SpEL expression over the method's arguments is a
reasonable declarative alternative once this kind of check appears in several places, but
introduce it deliberately rather than by habit — it moves the rule out of the method body and
into an annotation, which is harder to unit test in isolation.

## 9. Verify

| Check | Expectation |
|---|---|
| Page renders | `/approval/queue` returns the queue with header and sidebar |
| Link resolves | A row link reaches `/approval/detail?id=…` |
| Bad parameter | `?id=abc` shows a validation message, not a stack trace |
| Form posts correctly | The rendered `action` attribute equals the page URL |
| Authorisation | An employee account is refused; an administrator is admitted, and the refusal appears as an `ACCESS_DENIED` row |
| Atomicity | The `approval_actions` row and the `activity_log` row appear together, or not at all |
| Refusal durability | A refused action leaves a `DENIED` row **and** no mutation |
| Query count | With `show-sql=true`, the queue issues two statements, not one per row |

The atomicity check is worth performing deliberately: temporarily break the activity-log
SQL and confirm that **no** approval action is recorded. If one is, the transaction
boundary is in the wrong place.

These two checks are now also encoded as tests (`AuditInvariantsTest`, `AuditAtomicityTest`)
and should be extended rather than re-performed by hand for each new mutation. When adding
one, verify it can fail: break the rule in the production code deliberately and confirm the
test goes red. A test that cannot fail is worse than no test, because it reads like cover.

The refusal check is its mirror image and is just as easy to get wrong in the opposite
direction: trigger the refusal, then confirm both that the `DENIED` row exists and that the
mutation did not happen. A `DENIED` row that never appears means the write joined the
rolled-back transaction — the entry was saved without `.refused(...)`, which is what
selects the `REQUIRES_NEW` path.

## 10. Conventions Reference

| Concern | Standard |
|---|---|
| Page location | `webapp/<domain>/<page>.xhtml`; the path is the URL |
| Bean name | `<Domain><Page>View` — for example `ApprovalQueueView` |
| Bean package | `bean/<domain>/` |
| Read-only view | `@RequestScoped` |
| View containing a form | `@ViewScoped` + `Serializable` + `serialVersionUID` |
| Injected service in a `@ViewScoped` bean | `transient` |
| URL parameters | `f:viewParam` with `f:viewAction` |
| Links | `h:link outcome=`; never a literal `href` |
| Navigation after an action | `return "/x/y?faces-redirect=true";` |
| Navigation after a filter | add `&includeViewParams=true` to keep the URL bookmarkable |
| Forms | `h:form`; never a plain `<form>`, except the Spring Security login POST |
| Cancel buttons | `immediate="true"`, so they work while validation is failing |
| Conditional display | `rendered="#{...}"`; never JavaScript toggling `disabled` |
| Transactions | `@Transactional` on service methods only |
| Activity-log writes | `AuditTrail` only; `ActivityLogDao` is not called directly outside it |
| Actor identity into a service | One `Actor` parameter, built by the bean via `Actor.current()` |
| Data returned to views | DTO records; never JPA entities |
| SQL | Native SQL in DAOs only, one statement per method |
| Pagination | `LIMIT`/`OFFSET` in SQL, with a matching `COUNT(*)` |
| Pagination bean fields | `currentPage`, `totalPages`, `totalRecords`, `pageSize`, `offset` — standardized project-wide; not `page`/`totalPageCount`/`totalCount` (those read as near-duplicates at a glance) |
| `jakarta.faces` imports | Permitted in `bean/` only |
| Document tags | Always `h:head` and `h:body`, never plain `<head>`/`<body>` |

## 11. Anti-patterns

| Anti-pattern | Why it hurts | Correct mechanism |
|---|---|---|
| Logic inside `#{...}` beyond a boolean test | Runs on every render pass, untestable | A getter or a DTO field |
| A getter that queries the database | Getters are called many times per render | Load in `f:viewAction`, store in a field |
| `@RequestScoped` on a view with a form | State cannot survive a postback | `@ViewScoped` |
| JavaScript toggling `disabled` or `readonly` | The server never learns; nothing is validated | `rendered="#{bean.editing}"` |
| A plain `<form>` in a JSF page | No view state, no lifecycle, no validation | `h:form` |
| Returning a JPA entity to a view | Lazy loading during render produces one query per row | A DTO record |
| Business rules in the backing bean | Outside any transaction, unreachable from other callers | The service |
| `@Transactional` on a DAO method | A transaction spans one statement rather than one action | The service method |
| Building an `ActivityLog` by hand at a call site | Every site then re-decides `log` vs `logRefusal`; getting it backwards loses the row silently | `AuditTrail` |
| A service calling `Actor.current()` or `SecurityUtil` | Binds the service to a live web request, so it can no longer run from a job, a test, or an event listener | The bean resolves the `Actor` and passes it in |
| `==` between two boxed `Long` ids | Compares references. It appears to work below 128, where the `Long` cache returns one instance, then fails silently above it | `.equals(...)`, or `Objects.equals(...)` when either side may be null |

## 12. Known Pitfalls

Specific, non-obvious failures encountered while building this project, kept here so they
are not rediscovered at the same cost. Each is real: hit once, understood, and fixed.

### Rendering and Facelets

| Pitfall | What actually happens | Fix |
|---|---|---|
| `rendered` on a plain HTML element, e.g. `<li rendered="#{...}">` | Does nothing. The browser ignores an unrecognized attribute and always shows the element — the page renders without error, the EL is even evaluated, and the output contains a literal `rendered="false"` on an element that is plainly visible | Wrap the element in `<ui:fragment rendered="...">`, or use an `h:` component that actually supports the attribute. Grep the rendered HTML for `rendered="false"` to catch a slip — if it appears in the output, it didn't work |
| HTML5's `selected`/`disabled`/`checked` on a plain (non-`h:`) element | These are presence-based, not value-based. `selected="#{someBoolean}"` renders the literal text `selected="false"` when false, which the browser still treats as selected because the attribute is present at all | Make the EL expression return the string `'selected'` or `null` instead of a boolean; Facelets omits a `null`-valued attribute on a plain element |
| An EL expression on a plain element's attribute that evaluates to an empty string | Facelets omits the attribute entirely, not just when the result is `null`. A `value=""` sentinel (e.g. a "no filter" `<option>`) silently loses its `value` attribute, and the browser falls back to submitting the option's visible text instead | If an attribute genuinely needs to render as `""`, write it as a literal (non-EL) attribute rather than one that happens to evaluate to empty |
| `h:` component tags vs. plain HTML tags on the same page | `h:` components use `styleClass`; plain HTML tags (a CDN `<link>`/`<script>`, for instance) use ordinary `class` | Both exist side by side on some pages — know which kind of tag you're styling |
| Firefox vs. Chrome on a malformed tag | Firefox parses JSF pages as strict XML — a malformed tag anywhere produces a raw XML parser error. Chrome degrades to a normal, if broken-looking, render | If a page looks fine in Chrome but dumps a parse error in Firefox, the page has a real markup bug — fix the tag, it is not a browser quirk |
| A CDN `<script>`/`<link>` with an `integrity`/`crossorigin` (SRI) attribute that doesn't match the served file | The whole resource silently fails to load. No visible network error — just a Console warning — and the page renders unstyled | Only pin an SRI hash verified against the exact file being served, or drop the attribute |
| A Java-side redirect or hand-built URL pointing at a Facelets page | Needs the real `.xhtml` extension — there is no JSP-style clean-URL forwarding for Facelets from Java code | Prefer `h:link outcome=`/a `faces-redirect=true` navigation string over a hand-built URL in the first place |
| The page-view URL and the form-submission target | Two separate things. Making a URL render a page does not make that page's form submit back to the same URL | Update the form's target deliberately when a view's URL changes — it does not follow automatically |

### Composite components (`webapp/resources/ats/`)

| Pitfall | What actually happens | Fix |
|---|---|---|
| A composite component's own root `<ui:component>` tag with no `xmlns:ui` on itself | Fails to parse — "the prefix 'ui' ... is not bound" — even if nothing else in the file uses another `ui:` tag | Declare `xmlns:ui="jakarta.faces.facelets"` on the root element itself; the prefix used there needs its own binding |
| `for` as a `cc:attribute` name | Fails to parse as `#{cc.attrs.for}` — `for` is a reserved word in Java/EL's tokenizer | Avoid Java keywords as composite-component attribute names generally (this project uses `fieldId` instead) |
| `f:param`/`<option>` elements generated inside `ui:repeat` | Land as children of `ui:repeat` itself, not of the surrounding `h:outputLink`/`select` — and those renderers only scan their own direct children when building output | Use `xmlns:c="jakarta.tags.core"`'s `c:forEach` instead, as `pagination.xhtml` and `filterSelect.xhtml` do. It is Facelets' own build-time-only tag, bundled in Mojarra — no JSTL/JSP dependency, and no exception to the project's JSP+JSTL deferral beyond this one "must produce literal direct children" case. Don't reach for it elsewhere |

### Data access

| Pitfall | What actually happens | Fix |
|---|---|---|
| A native-query SQL string written as a `"""` text block, with a trailing `;` | A SQL syntax error — Hibernate sends the string through JDBC as a single statement | Omit the trailing semicolon inside a text-block query |
| Manually mapping a native query's `Object[]` row into a Java object | A nullable column throws or silently corrupts if cast without a check; the JDBC driver's actual runtime type for a numeric column also isn't guaranteed | `row[i] == null ? null : ((Number) row[i]).longValue()` — check null and cast via `Number`, never a direct `(Long)` |

### Transactions

| Pitfall | What actually happens | Fix |
|---|---|---|
| `jakarta.transaction.Transactional` vs. `org.springframework.transaction.annotation.Transactional` | Only the Spring one does anything here — this application has no JTA transaction manager (its own boot log states `WELD-000101: Transactional services not available`), so the `jakarta.transaction` version on a `@Service` method is very likely a silent no-op. An IDE auto-import can pick the wrong one with no compile error | Import the Spring annotation explicitly. Don't trust a multi-statement method's atomicity until it has been proven by fault injection — break one statement deliberately and confirm the others roll back too |
