# JSF Implementation Guide — Asset Tagging System

A complete, practical reference for every JSF pattern needed to finish this project — not just syntax basics, but the specific implementations Step 4 and Step 7 of [docs/development-plan.md](development-plan.md) still need: forms that write to the database, conditional rendering by role, confirm-before-destructive-action, file upload, and cleaning up the header/sidebar duplication that exists today.

**Stack, and why every example below is written exactly this way:** Jakarta Faces 4.1 (Mojarra implementation) via JoinFaces 6.1, inside Spring Boot 4.1, with Weld as the CDI runtime. Every example uses `jakarta.faces.*` namespaces (not the pre-2020 `javax.faces.*`), CDI `@Named` beans (not the old `@ManagedBean`), and — where a page needs Bootstrap 5 classes on a JSF component — `styleClass`, not `class`. This is not a generic JSF tutorial; every pattern below is chosen because this specific project needs it, and every project-specific example is written against this codebase's actual conventions (`FacesUtil`, `PageParams`, `OptionalUtils`, the DAO layer), not invented ones.

**2026-08-17 rewrite.** The previous version of this guide covered the fundamentals only, written before Step 4/7's forms, conditional visibility, file upload, and confirm-dialog work existed as concrete near-term tasks. This version adds everything needed to actually build them.

## Contents

**Fundamentals**
1. [Anatomy of a page](#1-anatomy-of-a-page)
2. [Expression language](#2-expression-language)
3. [Managed beans and CDI scopes](#3-managed-beans-and-cdi-scopes)
4. [A full worked example](#4-a-full-worked-example)
5. [The h: tag library](#5-the-h-tag-library)
6. [The f: tag library](#6-the-f-tag-library)
7. [Navigation](#7-navigation)
8. [Displaying a list](#8-displaying-a-list)

**Validation**
9. [Validation and error redisplay](#9-validation-and-error-redisplay)
10. [Bean Validation vs. JSF validators vs. custom validators](#10-bean-validation-vs-jsf-validators-vs-custom-validators)

**What this project still needs to build**
11. [Forms that write to the database — the full pattern](#11-forms-that-write-to-the-database--the-full-pattern)
12. [Conditional rendering: role-gated and state-gated content](#12-conditional-rendering-role-gated-and-state-gated-content)
13. [Confirm-before-destructive-action](#13-confirm-before-destructive-action)
14. [Templating: fixing the header/sidebar duplication](#14-templating-fixing-the-headersidebar-duplication)
15. [File upload and download](#15-file-upload-and-download)
16. [AJAX with f:ajax — when to reach for it here](#16-ajax-with-fajax--when-to-reach-for-it-here)
17. [View scope: when RequestScoped stops being enough](#17-view-scope-when-requestscoped-stops-being-enough)

**Reference**
18. [Common gotchas](#18-common-gotchas)
19. [Jakarta Faces 4.1-specific facts worth knowing](#19-jakarta-faces-41-specific-facts-worth-knowing)
20. [What this guide doesn't cover](#20-what-this-guide-doesnt-cover)

---

## 1. Anatomy of a page

Every Facelets page is well-formed XML. The root `<html>` element declares which tag libraries you're importing as XML namespaces — this replaces what a `<%@ taglib %>` directive would do in JSP.

```xml
<!-- src/main/webapp/hello.xhtml -->
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html"
      xmlns:f="jakarta.faces.core">
<h:head>
    <title>My Page</title>
</h:head>
<h:body>
    <h:outputText value="Hello, JSF" />
</h:body>
</html>
```

`xmlns:h` and `xmlns:f` are the two tag libraries you'll use on nearly every page — `h:` for HTML-rendering components (forms, inputs, tables), `f:` for non-rendering helpers (validators, converters, facets). Two more appear later in this guide once they're needed: `xmlns:ui` (`jakarta.faces.facelets` — templating, §14) and `xmlns:p` (`jakarta.faces.passthrough` — already used in this project for `p:type="date"` on the purchase-date field). Only declare the namespaces a given page actually uses.

> **Note the namespace URIs.** Tutorials written before the `javax` → `jakarta` rename use `http://xmlns.jcp.org/jsf/html` instead of `jakarta.faces.html`. Same tag library, old address — mentally swap it whenever you're reading older material, including most Stack Overflow answers from before 2022.

## 2. Expression language

EL is how a page talks to a bean. JSTL's EL (`${...}`) is immediate — evaluated once. JSF uses a different bracket on purpose: `#{...}` is *deferred* — it can be re-evaluated at several points in the request lifecycle, and, critically, it can be **written to** as well as read. That's what makes two-way form binding possible.

| Expression | What it does |
|---|---|
| `#{bean.name}` | Reads `getName()`; on a form submit, calls `setName(value)` |
| `#{bean.save}` | Calls the no-arg method `save()` when a button/link fires |
| `#{bean.approve(request)}` | Calls a method taking an argument — legal in EL 3.0+, used in §11 |
| `#{not empty bean.list}` | True if a collection/string isn't null or empty — the standard "do I have anything to show" check |
| `#{bean.role == 'ROLE_ADMIN'}` | A boolean comparison, directly in EL — used constantly in §12 for role gating |

## 3. Managed beans and CDI scopes

A managed bean is a plain Java object a page's EL expressions point at. In this stack, that means a CDI bean — `@Named` plus a scope annotation. (Weld, the CDI runtime, starts automatically via JoinFaces — every existing bean in `bean/` already uses this style.)

| Scope | Import | Lives for |
|---|---|---|
| `@RequestScoped` | `jakarta.enterprise.context` | One HTTP request — recreated every time. Everything in this project uses this today. |
| `@ViewScoped` | `jakarta.faces.view` | As long as the user stays on the same page, across postbacks. Not used anywhere in this project yet — §17 explains exactly when it would start mattering and why it hasn't so far. |
| `@SessionScoped` | `jakarta.enterprise.context` | The user's whole logged-in session |
| `@ApplicationScoped` | `jakarta.enterprise.context` | The whole app's lifetime, one shared instance — `LookupBean` uses this for department/category dropdowns that rarely change |

The bean's name in EL is its class name with a lowercase first letter, unless `@Named` is given an explicit value: `AssetTransferBean` → `#{assetTransferBean...}`.

## 4. A full worked example

Everything above, combined: a name field, a button, and a greeting that appears after submit.

```java
// GreetingBean.java
package com.sil.asset_tagging_system.web;

import jakarta.inject.Named;
import jakarta.enterprise.context.RequestScoped;

@Named
@RequestScoped
public class GreetingBean {

    private String name;
    private String greeting;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGreeting() { return greeting; }

    public String sayHello() {
        greeting = "Hello, " + name + "!";
        return null; // null/same view = stay on this page
    }
}
```

```xml
<!-- src/main/webapp/greeting.xhtml -->
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html">
<h:body>
    <h:form>
        <h:outputLabel for="name" value="Your name:" />
        <h:inputText id="name" value="#{greetingBean.name}" required="true" />
        <h:message for="name" style="color:red" />
        <h:commandButton value="Say Hello" action="#{greetingBean.sayHello}" />

        <h:outputText value="#{greetingBean.greeting}"
                      rendered="#{not empty greetingBean.greeting}" />
    </h:form>
</h:body>
</html>
```

Trace it through: page loads → `getName()` returns null, field is empty. Type a name, click the button → JSF calls `setName(...)` before the button's action runs → `sayHello()` sets `greeting` → the page re-renders, and now `#{not empty greetingBean.greeting}` is true, so the greeting appears. Leave the name blank and submit → `required="true"` stops it before `sayHello()` ever runs, and `h:message` shows why.

## 5. The h: tag library

| Tag | Renders as | Use it for |
|---|---|---|
| `h:form` | `<form>` | Wraps every input/button — nothing submits without it |
| `h:inputText` | `<input type="text">` | Single-line text, two-way bound |
| `h:inputTextarea` | `<textarea>` | Multi-line — request reasons, rejection notes (§11, §12) |
| `h:inputSecret` | `<input type="password">` | Passwords |
| `h:selectOneMenu` | `<select>` | Dropdowns — pair with `f:selectItems`, see §6 |
| `h:selectBooleanCheckbox` | `<input type="checkbox">` | A single true/false toggle, bound to a `boolean` property |
| `h:commandButton` | `<input type="submit">` | Submits the enclosing form and fires an `action` |
| `h:commandLink` | `<a>` that submits a form | Same as `h:commandButton`, styled as a link — used for row-level "View"/"Approve" links inside a table |
| `h:outputLink` | `<a>` | A plain navigational link (GET, no form submission) — already used throughout this project for "Add Asset", pagination |
| `h:outputText` | plain text | Displaying a value, optionally with `rendered` |
| `h:panelGroup` | `<span>` or `<div>` (via `layout="block"`) | A wrapper when you need one EL-aware container around several components — useful inside a `rendered` block |
| `h:graphicImage` | `<img>` | An image bound to a resource or a dynamic URL |
| `h:message` / `h:messages` | error text | Validation feedback, per-field or all-at-once |
| `h:dataTable` | `<table>` | Rendering a list — see §8 |
| `h:inputFile` | `<input type="file">` | File upload — see §15 |

> **`h:inputText`, `h:commandButton`, and every other input/command component only work inside an `h:form`.** Outside one, there's nothing for JSF to submit values back into.

## 6. The f: tag library

These don't render anything themselves — they attach behavior to the `h:` tag they sit inside.

```xml
<!-- Populating a dropdown from a list -->
<h:selectOneMenu value="#{bean.departmentId}">
    <f:selectItem itemLabel="-- Select --" itemValue="#{null}" />
    <f:selectItems value="#{lookupBean.departmentList}"
                   var="d" itemLabel="#{d.name}" itemValue="#{d.id}" />
</h:selectOneMenu>
```

```xml
<!-- Populating a dropdown from a Java enum — the pattern Step 7 needs for
     an AssetCondition selector. f:selectItems can iterate an array directly. -->
<h:selectOneMenu value="#{assetDetailBean.newCondition}">
    <f:selectItems value="#{assetDetailBean.selectableConditions}" var="c"
                   itemLabel="#{c}" itemValue="#{c}" />
</h:selectOneMenu>
```
```java
// On the bean: only offer conditions an admin may pick directly.
// ASSIGNED/RETIRED-equivalent states are side effects of other actions,
// never a raw dropdown choice — see docs/development-plan.md Step 7.3.
public AssetCondition[] getSelectableConditions() {
    return new AssetCondition[] { AssetCondition.IN_SERVICE, AssetCondition.DAMAGED, AssetCondition.UNDER_MAINTENANCE };
}
```

```xml
<!-- A validator beyond just required="true" — already used nowhere in this
     project yet, but ready for Step 7.5's server-side validation -->
<h:inputText value="#{assetFormBean.assetTag}">
    <f:validateLength minimum="3" maximum="50" />
</h:inputText>
```

Other `f:` tags worth knowing exist, even before you need them:

| Tag | Purpose |
|---|---|
| `f:convertDateTime` | Already used on `add-asset.xhtml`'s purchase-date field — converts between the `LocalDate` bean property and the text the browser's date picker sends |
| `f:viewParam` | Reads a query-string parameter into a bean property as the view builds — an alternative to this project's current convention of reading params manually via `FacesUtil.getRequestParams()` in `@PostConstruct`. Worth knowing both exist; this project deliberately uses the manual style everywhere, so match it rather than mixing conventions on one page. |
| `f:metadata` | The wrapper `f:viewParam` needs to sit inside, placed in `<h:head>` |
| `f:param` | Adds a query parameter to an `h:outputLink`/`h:commandLink` — already used for pagination (`<f:param name="page" value="..."/>`) |
| `f:facet` | Names a named slot inside a component — `h:dataTable`'s column headers use this already |
| `f:ajax` | Attaches AJAX behavior to a component — see §16 |
| `f:validateBean` | Forces Bean Validation (`jakarta.validation`, `@NotNull`/`@Size`/etc.) to run for a specific component — see §10 |

## 7. Navigation

An action method's return value tells JSF what to show next.

| Return value | Result |
|---|---|
| `null` or the current view's name | Stay on the same page (what `sayHello()` does above) |
| `"assets"` | Forward to `assets.xhtml` — URL bar doesn't change |
| `"assets?faces-redirect=true"` | A real HTTP redirect — URL bar updates, back button behaves correctly |

Default to the `?faces-redirect=true` form after anything that changes data (a save, an approval decision) — same reasoning as any redirect-after-POST pattern: it stops a page refresh from resubmitting the form. This project's existing controllers additionally do their own redirect-based navigation for clean URLs (`AssetController`, `UserController` — see [docs/SITE_MAP.md](SITE_MAP.md)); the two mechanisms aren't in conflict, they operate at different points in the request (controller-level clean-URL forwarding vs. bean-level post-action navigation).

## 8. Displaying a list

Bind `h:dataTable` to any `List<T>` property on a bean; `var` names the current row inside the loop.

```xml
<h:dataTable value="#{bean.items}" var="item" styleClass="table table-striped">
    <h:column>
        <f:facet name="header">Name</f:facet>
        #{item.name}
    </h:column>
    <h:column>
        <f:facet name="header">Status</f:facet>
        #{item.status}
    </h:column>
</h:dataTable>
```

This is a genuine JSF component, not a build-time loop — the reason it's the safe choice for rows that include form inputs or command links, unlike `ui:repeat` (Facelets' lighter-weight looping tag, fine for read-only display, riskier once a row contains its own button — see the gotcha in §18).

## 9. Validation and error redisplay

This is the behavior JSF hands you without writing it yourself: submit a form with a validation failure, and the page redisplays with every field the user already typed still filled in, plus `h:message` showing what's wrong — the action method never even runs. Compare this to hand-rolling that redisplay logic in a plain servlet; it's a large part of the reason to reach for JSF on a form at all.

## 10. Bean Validation vs. JSF validators vs. custom validators

Three distinct mechanisms exist, and this project's `validation/ValidationConstants.java` (regex patterns, length limits) is currently unused — it was written for whichever of these gets picked. Know all three before choosing:

**Bean Validation (`jakarta.validation`)** — annotations directly on a bean's fields (`@NotBlank`, `@Size(min=3, max=50)`, `@Pattern`). `spring-boot-starter-validation` is already a project dependency. Runs automatically for `@Valid`-annotated Spring MVC parameters, but for a JSF-bound property it needs an explicit `<f:validateBean/>` on the component, or `javax.faces.validator.BEAN` disabled/enabled correctly — this is the natural home for `ValidationConstants`' patterns:

```java
public class AssetFormBean {
    @Pattern(regexp = "^[A-Z]{3}-\\d{4}$", message = "Asset tag must look like AST-1001")
    private String assetTag;
    // ...
}
```
```xml
<h:inputText value="#{assetFormBean.assetTag}">
    <f:validateBean />
</h:inputText>
```

**Built-in JSF validators (`f:validateLength`, `f:validateRegex`, `f:validateLongRange`)** — no annotation needed, declared directly in the page (§6 above). Simpler for a one-off, page-specific rule; less reusable than Bean Validation if the same rule applies in more than one place.

**Custom validator (`@FacesValidator`)** — for a rule that needs a database check (duplicate asset tag, in-use email) or genuinely custom logic that doesn't fit a regex:

```java
package com.sil.asset_tagging_system.validation;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
import jakarta.inject.Inject;

import com.sil.asset_tagging_system.dao.AssetDao;

@FacesValidator(value = "assetTagUniqueValidator", managed = true) // managed=true lets @Inject work
public class AssetTagUniqueValidator implements Validator<String> {

    @Inject
    private AssetDao assetDao;

    @Override
    public void validate(FacesContext context, UIComponent component, String value) {
        if (assetDao.existsByAssetTagIgnoreCase(value)) {
            throw new ValidatorException(
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Asset tag already exists", null));
        }
    }
}
```
```xml
<h:inputText value="#{assetFormBean.assetTag}">
    <f:validator validatorId="assetTagUniqueValidator" />
</h:inputText>
```

`managed = true` is the detail worth remembering — without it, JSF instantiates the validator itself and `@Inject` fields stay null. This moves `AssetFormBean.save()`'s current manual `existsByAssetTagIgnoreCase` check (a plain `if` + `FacesMessage`, still perfectly valid JSF) into a reusable, page-declarative form — a genuine choice, not a strict requirement; the manual-check style already in `AssetFormBean` is not wrong, just less reusable if a second page ever needs the same check.

## 11. Forms that write to the database — the full pattern

This project already has one complete, working example of this pattern: `AssetFormBean.save()` + `add-asset.xhtml`. Read those two files alongside this section — this is the shape every future write (Employee edit, asset transfer, approval decision) should follow.

**The five-part shape:**

1. **Bean fields, one per form input**, plain getters/setters (Lombok `@Getter @Setter` in this project's style).
2. **A no-arg action method** (or one taking a simple argument, per §2) that: validates business rules JSF's own validators can't express (duplicate checks, cross-field rules), performs the write via a DAO, records the event (once Step 2's `activity_log` DAO exists — see [docs/development-plan.md](development-plan.md) Step 2), and returns a redirect string.
3. **The current user**, read from Spring Security, never from a hidden form field — `AssetFormBean.getCurrentUserId()` is the established pattern:
   ```java
   private Long getCurrentUserId() {
       Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
       return (principal instanceof CustomUserDetails userDetails) ? userDetails.getUserId() : null;
   }
   ```
4. **A `FacesMessage` on failure**, added to the null component (a global message, shown by `<h:messages globalOnly="true"/>`) rather than a specific field, when the failure isn't a single-field validation problem:
   ```java
   FacesContext.getCurrentInstance().addMessage(null,
       new FacesMessage(FacesMessage.SEVERITY_ERROR, "Asset tag already exists", null));
   return null; // stay on the page, message renders
   ```
5. **A redirect on success**, via `ExternalContext.redirect(...)` to the clean controller URL (not the `.xhtml` view) — this project's established style, rather than JSF's own `?faces-redirect=true` navigation string:
   ```java
   FacesContext.getCurrentInstance().getExternalContext()
       .redirect(getRequest().getContextPath() + "/assets");
   ```

**Applying this to Step 7's approval decision** (sketch, not a finished class — the actual `approval_actions` write logic is Step 7's to design, per [docs/DESIGN.md](DESIGN.md) §4):

```java
@Named
@RequestScoped
public class ApprovalDetailBean {

    private final ApprovalDao approvalDao;
    private Long id;
    private Approval approval;   // however this is loaded, per Step 3.5's entity fix
    private String rejectionNotes;

    // ... @PostConstruct loads `approval` from the id query param, FacesUtil-style ...

    public String approve() {
        Long currentUserId = getCurrentUserId();
        approvalDao.recordAction(id, currentUserId, "APPROVED", null); // sketch signature, see development-plan.md 7.3
        return null; // or redirect, depending on whether the queue or the detail page should show next
    }

    public String reject() {
        if (rejectionNotes == null || rejectionNotes.isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "A reason is required to reject a request", null));
            return null;
        }
        approvalDao.recordAction(id, getCurrentUserId(), "REJECTED", rejectionNotes);
        return null;
    }
}
```
```xml
<h:form>
    <h:messages globalOnly="true" styleClass="alert alert-danger" layout="table"/>

    <h:commandButton value="Approve" action="#{approvalDetailBean.approve}"
                     styleClass="btn btn-success"
                     rendered="#{approvalDetailBean.canApprove}" />

    <h:inputTextarea value="#{approvalDetailBean.rejectionNotes}"
                     styleClass="form-control mb-2" rows="2"
                     rendered="#{approvalDetailBean.canApprove}" />
    <h:commandButton value="Reject" action="#{approvalDetailBean.reject}"
                     styleClass="btn btn-danger"
                     rendered="#{approvalDetailBean.canApprove}" />
</h:form>
```

`canApprove` above is a bean method combining role and state (exactly the pattern in §12) — this is what replaces the old design's "hide the button if you already gave the first approval" UI logic, now backed by `approval_actions`'s `UNIQUE (approval_id, actor_user_id)` constraint doing the actual enforcement at the database level (per [docs/DESIGN.md](DESIGN.md) §4) rather than the bean being the only thing standing between a user and a double approval.

## 12. Conditional rendering: role-gated and state-gated content

Every `h:` component and `ui:fragment` accepts a `rendered` attribute — a boolean EL expression deciding whether the component exists in the response at all (not just hidden with CSS — a `rendered="false"` component is never sent to the browser).

**Role gating**, using the project's existing `HeaderBean.getRole()`:

```xml
<!-- Only an admin sees who currently holds this asset -->
<ui:fragment rendered="#{headerBean.role == 'ROLE_ADMIN'}">
    <p>Currently held by: #{assetDetailBean.currentHolder.fullName}</p>
</ui:fragment>
```

`ui:fragment` (from the `ui:` templating namespace, `jakarta.faces.facelets`) is the right choice when there's no natural `h:` component to hang `rendered` off of — it renders nothing itself, just conditionally includes its children. This project already uses this exact pattern in `user-detail.xhtml` and `asset-log.xhtml`.

**State gating**, combining role and status — the pattern §11's `canApprove` needs:

```java
public boolean isCanApprove() {
    boolean isAdmin = "ROLE_ADMIN".equals(headerBean.getRole());
    boolean isOpen = approval.getStatus() == ApprovalStatus.PENDING
                   || approval.getStatus() == ApprovalStatus.PARTIALLY_APPROVED;
    return isAdmin && isOpen;
}
```

Note the getter name: a boolean property `canApprove` needs `isCanApprove()` (JavaBean convention for `boolean`, not `Boolean`) for `#{bean.canApprove}` to resolve — a real, easy-to-miss gotcha, listed again in §18.

**Two components with mutually exclusive `rendered` conditions**, the pattern this project already uses for pagination Previous/Next and for the pending-transfer banner:

```xml
<ui:fragment rendered="#{assetDetailBean.transferPending}">
    <div class="alert alert-info">A transfer request is already pending for this asset.</div>
</ui:fragment>
<ui:fragment rendered="#{!assetDetailBean.transferPending}">
    <h:outputLink value="/assets/#{assetDetailBean.id}/transfer" styleClass="btn btn-primary">
        Transfer
    </h:outputLink>
</ui:fragment>
```

## 13. Confirm-before-destructive-action

Step 7.4 (retire an asset) needs this, and Step 6 of the development plan already names it as the flagship example of "raw JS as needed." The pattern is a plain `onclick` returning `false` to cancel the submit — no library, no `f:ajax`, just the browser's built-in `confirm()`:

```xml
<h:commandButton value="Retire Asset" action="#{assetDetailBean.retire}"
                 styleClass="btn btn-outline-danger"
                 onclick="return confirm('Retire this asset? This cannot be undone from this page.');" />
```

If `confirm()` returns `false`, the browser never submits the form, and `retire()` never runs — the cancellation happens entirely client-side, before any request is sent. This is genuinely all Step 7.4 needs; reach for a proper JS confirmation modal (Bootstrap's own, or a small custom one) only if the plain browser dialog's styling becomes a real complaint, not preemptively.

For a *reusable* confirm dialog across several buttons (rather than one inline `onclick` per button), a tiny shared JS function is worth it once there's a second use case:

```js
// src/main/webapp/resources/js/confirm-action.js
function confirmDestructive(message) {
    return confirm(message);
}
```
```xml
<h:outputScript name="confirm-action.js" library="js" />
<!-- ... -->
<h:commandButton value="Retire" action="#{bean.retire}"
                 onclick="return confirmDestructive('Retire this asset?');" />
```

`h:outputScript name="..." library="js"` is this project's established convention for serving JS (already used by `user-detail.js`) — the JSF resource-library mechanism, not a raw `<script src="...">`, which is why the file lives under `webapp/resources/js/` rather than anywhere else (anything under `WEB-INF/` is unreachable by the browser regardless — see [docs/ARCHITECTURE.md](ARCHITECTURE.md) §11).

## 14. Templating: fixing the header/sidebar duplication

**The problem, concretely.** Every existing page repeats this same block:

```xml
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet" .../>
<link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet" />
<!-- ... -->
<ui:include src="/WEB-INF/templates/header.xhtml"/>
<div class="row">
    <ui:include src="/WEB-INF/templates/sidebar.xhtml"/>
    <main class="col-9">
        <!-- page content -->
    </main>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js" .../>
```

Nine `.xhtml` files, nine copies of the same CDN links and layout scaffolding. Every new page (and every future CDN version bump) means editing all nine. `ui:include` — what's used today — pulls in a fragment's markup, but has no concept of "the including page fills in a slot"; it can't express "here's the shared shell, and here's what changes per page."

**`ui:composition` + `ui:decorate` solve exactly this.** A composition/decoration owns a *template* page with named insertion points (`ui:insert`); each real page becomes a thin file that only supplies the parts that differ (`ui:define`), and everything else — the `<head>`, the CDN links, the header/sidebar include, the closing scripts — lives in exactly one file.

**The template**, new file `WEB-INF/templates/page.xhtml`:

```xml
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html"
      xmlns:ui="jakarta.faces.facelets">
<h:head>
    <title><ui:insert name="title">Asset Tagging System</ui:insert></title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous"/>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet" />
</h:head>
<h:body styleClass="bg-body text-body">
    <div class="container">
        <ui:include src="/WEB-INF/templates/header.xhtml"/>
        <div class="row">
            <ui:include src="/WEB-INF/templates/sidebar.xhtml"/>
            <main class="col-9">
                <ui:insert name="content">Page content goes here.</ui:insert>
            </main>
        </div>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js"
            integrity="sha384-FKyoEForCGlyvwx9Hj09JcYn3nv7wiPVlz7YYwJrWVcXK/BmnVDxM+D2scQbITxI" crossorigin="anonymous"></script>
</h:body>
</html>
```

**Any real page** then becomes a `ui:composition` referencing it — `dashboard.xhtml` as a worked example:

```xml
<ui:composition xmlns="http://www.w3.org/1999/xhtml"
                xmlns:ui="jakarta.faces.facelets"
                template="/WEB-INF/templates/page.xhtml">
    <ui:define name="title">Dashboard</ui:define>
    <ui:define name="content">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div class="gap-2 p-2">
                <h2 class="text-primary fw-bold">Dashboard</h2>
                <p class="text-secondary text-muted">Welcome welcome</p>
            </div>
        </div>
    </ui:define>
</ui:composition>
```

Everything outside `<ui:composition>` in this file is **ignored by Facelets entirely** — including any `<html>`/`<head>` you might reflexively add. This trips up almost everyone the first time: a `ui:composition` file only needs the namespace declarations and the two `ui:define` blocks; no `<h:head>`, no CDN links, nothing else. `ui:decorate` is the same mechanism with one difference — a `ui:decorate` file can contain content *outside* its `ui:define` blocks (useful for a page needing extra content before/after the templated slot); `ui:composition` discards everything outside its own tags. Start with `ui:composition`; reach for `ui:decorate` only if a specific page genuinely needs that flexibility.

**This is a real refactor, not required to finish any single feature** — every existing page keeps working exactly as it does today without it. Worth doing once, ideally before Step 7 adds five more pages that would otherwise each need their own copy-pasted `<head>`.

## 15. File upload and download

Step 7.6, the last thing standing between `AssetDocument`'s `LONGBLOB` columns and an actual working feature.

**Upload — `h:inputFile`, bound to a `jakarta.servlet.http.Part`:**

```xml
<h:form enctype="multipart/form-data">
    <h:inputFile value="#{assetDocumentBean.uploadedImage}" />
    <h:commandButton value="Upload" action="#{assetDocumentBean.saveImage}" />
</h:form>
```
```java
import jakarta.servlet.http.Part;

@Named
@RequestScoped
public class AssetDocumentBean {

    private Part uploadedImage;

    public Part getUploadedImage() { return uploadedImage; }
    public void setUploadedImage(Part uploadedImage) { this.uploadedImage = uploadedImage; }

    public String saveImage() throws IOException {
        byte[] bytes = uploadedImage.getInputStream().readAllBytes();
        String contentType = uploadedImage.getContentType();
        // assetDocumentDao.saveImage(assetId, bytes, contentType) -- Step 7.6's to write
        return null;
    }
}
```

**Configuration — this project should need none beyond what's already in place.** `h:inputFile` requires the servlet handling the request to accept multipart bodies. JoinFaces auto-configures the `FacesServlet` with multipart support against an embedded Tomcat (what this project runs), reading limits from Spring's own `spring.servlet.multipart.*` properties rather than needing a separate `@MultipartConfig` or manual filter registration — set `spring.servlet.multipart.max-file-size`/`max-request-size` in `application.properties` if the defaults are too small for asset photos or invoice PDFs, and treat this as verify-by-testing rather than a guarantee, since it hasn't been exercised in this codebase yet.

**Download — JSF genuinely cannot do this part; a plain Spring MVC endpoint is the correct tool, not a workaround:**

```java
@RestController
public class AssetDocumentController {

    private final AssetDocumentDao assetDocumentDao;

    @GetMapping("/assets/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        // AssetDocument doc = assetDocumentDao.findByAssetId(id)...
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(doc.getImageMimeType()))
            .body(doc.getAssetImage());
    }
}
```
```xml
<h:graphicImage value="/assets/#{asset.id}/image" alt="Asset photo" styleClass="img-thumbnail" />
```

EL can bind text and simple values into a page; it has no mechanism for streaming raw bytes through a component tree. A binary download or an inline image is always a direct HTTP response from a servlet endpoint, referenced by URL from the JSF page — the same architectural split this project already uses for controllers vs. views, just applied to bytes instead of markup.

## 16. AJAX with f:ajax — when to reach for it here

Every existing page in this project does a full postback: click a button, the whole page reloads. That's a deliberate match to the older JSF the target production system runs, not an oversight — see [docs/development-plan.md](development-plan.md)'s guiding principles. `f:ajax` exists and works in this stack, but introducing it anywhere should be a deliberate choice, not a reflex.

```xml
<!-- Re-render just the total-count badge when the search box changes,
     without a full page reload -->
<h:inputText value="#{assetBean.search}">
    <f:ajax event="keyup" render="totalCountBadge" listener="#{assetBean.filterCount}" />
</h:inputText>
<h:outputText id="totalCountBadge" value="#{assetBean.totalCount}" />
```

`execute` (what gets sent to the server — defaults to `@this`) and `render` (what gets updated in the response — defaults to `@none`) are the two attributes that matter. `@form` and `@all` are common values for both. Nothing in this project currently needs this — the full-postback convention is intentional — but it's the right tool if a future page genuinely needs partial updates (e.g., a live search-as-you-type result count) without abandoning the rest of the established style.

## 17. View scope: when RequestScoped stops being enough

Every bean in this project today is `@RequestScoped` and works because every form in this project does a single, complete postback — submit, the bean is thrown away, a fresh one handles the next request. This matches the development plan's own reasoning: "no `@ViewScoped` equivalent needed if the whole form posts and reloads each time, matching the legacy target system's constraints anyway."

`@ViewScoped` (`jakarta.faces.view.ViewScoped`, CDI-based, requires the bean to implement `Serializable`) exists and works fine in this stack if a future need genuinely calls for it — specifically, a **multi-step form that must retain state across more than one postback without a full page reload each time**, or an `f:ajax`-heavy page where re-creating the whole bean on every partial update would lose data the user already entered. Nothing in this project's current or near-term (Step 7) scope needs it — every approval/transfer form is a single submit, matching the existing convention — so don't introduce it speculatively. If a genuinely multi-step wizard ever gets designed, this is the annotation to reach for, and the choice should be as deliberate as the decision not to use it has been so far.

## 18. Common gotchas

- Using `${...}` instead of `#{...}` for a component binding — it renders once and never updates, since immediate EL can't be re-evaluated across the lifecycle.
- Putting `h:inputText` or `h:commandButton` outside an `h:form` — nothing happens on submit.
- A bean property with a getter but no setter, used in a *two-way* binding — JSF has nothing to call on submit and throws a property-not-writable error.
- **A `boolean` property needs `isX()`, not `getX()`**, for EL to resolve it as a boolean the JavaBean-conventional way — `getX()` returning a primitive `boolean` also works, but `isX()` is the convention every other JSF example (including this guide's §12) follows; pick one per property and don't mix.
- Forgetting a scope annotation entirely — CDI won't instantiate the bean, and `#{beanName...}` silently resolves to nothing, with no error in the logs pointing at the actual cause.
- **`ui:repeat` inside a form containing a per-row command button/link is fragile** — because it isn't a genuine `UIComponent` the way `h:dataTable` is, JSF can lose track of which row's button was actually clicked across a full postback in some cases. Use `h:dataTable` for any list that contains its own form controls (exactly what this project already does for `asset-list.xhtml`'s asset-tag links); `ui:repeat` is fine for pure read-only display.
- A `ui:composition`/`ui:decorate` file with markup outside its `ui:define` blocks that was meant to render — it's silently discarded for `ui:composition` (§14). If content isn't appearing, check it's inside a named `ui:define`.
- Copy-pasting a pre-2020 tutorial's `xmlns` values — old `javax`-era URIs, same tags, wrong address for this stack.
- File upload with no `enctype="multipart/form-data"` on the `h:form` — the file is silently never sent; the browser submits the form as ordinary URL-encoded data instead.

## 19. Jakarta Faces 4.1-specific facts worth knowing

Verified against the spec and BalusC's release notes (a JSF spec-committee member), not assumed from older-version habit:

- **`FACELETS_REFRESH_PERIOD` defaults to `0` automatically when `ProjectStage` is `Development`.** This is *why* `joinfaces.faces.project-stage=Development` (already set in `application.properties`) makes `.xhtml` edits show up without a restart — before 4.1 this had to be configured explicitly; now it's the specified default for that stage.
- CDI events (`@Initialized`, `@BeforeDestroyed`, `@Destroyed`) now fire for `@ViewScoped` — relevant only once §17's view scope is actually in use.
- A built-in `jakarta.faces.convert.UUIDConverter` now ships with the API — worth knowing if any future page ever needs to bind a `UUID` field directly (this project's own correlation-id work in `docs/DESIGN.md` handles UUIDs at the database layer, not through a JSF-bound field, so this hasn't come up yet).
- `<ui:repeat>` gained a `rowStatePreserved` attribute — a minor, rarely-needed knob for preserving per-row component state across data changes.

None of these change how any existing page in this project works; they're additions, not breaking changes from 4.0.

## 20. What this guide doesn't cover

Deliberately out of scope, either because this project has already decided against it or because nothing in the current plan needs it:

- **JSF 1.x-style XML-config managed beans** (`<managed-bean>` in `faces-config.xml`, no `@Named`) — explicitly deferred per [docs/development-plan.md](development-plan.md) Appendix B. Worth learning directly on the real legacy codebase this project prepares for, not practiced speculatively here.
- **Composite components** (`<cc:interface>`/`<cc:implementation>`, building a genuinely new reusable custom tag) — templating (§14) covers this project's actual duplication problem; a full composite component is a heavier tool than anything currently needed.
- **Faces Flow** (multi-page wizard navigation as a first-class JSF concept, `@Inject Flow`) — no multi-step wizard exists or is planned; §17 covers the lighter-weight `@ViewScoped` alternative if state-across-postbacks is ever needed without a full flow.
- **JSTL (`c:forEach`, etc.) on Facelets pages** — deferred per Appendix B; don't mix JSTL tags into any `.xhtml` page until that decision changes.
- **Getting a JSF bean to call a Spring-managed service** — this was an open question when this guide was first written; it's resolved (plain `@Inject` works with no bridge, confirmed in [docs/development-plan.md](development-plan.md) Step 1) and every example above already relies on it.
