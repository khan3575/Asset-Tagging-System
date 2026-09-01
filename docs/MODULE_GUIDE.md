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
raised themselves — belong on the service method as `@PreAuthorize`, as shown in Step 4.

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
| Pagination bean fields | `currentPage`, `totalPages`, `totalRecords`, `pageSize`, `offset` — standardized project-wide 2026-08-31; not `page`/`totalPageCount`/`totalCount` (those read as near-duplicates at a glance) |
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
