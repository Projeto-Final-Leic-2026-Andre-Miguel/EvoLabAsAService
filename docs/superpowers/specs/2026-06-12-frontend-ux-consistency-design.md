# EvoLab Frontend UX Consistency Design

## Scope

This change addresses reported UX issues 1, 2, 4-14 and two additional accessibility/content issues. Issue 3, credential validity being assumed on page load, is explicitly excluded and its current behavior must remain unchanged.

The implementation is limited to the existing React frontend and current backend contracts, except where frontend error handling consumes existing backend error responses. No schema change is required.

## Shared UI Components

### Confirmation Dialog

Create a reusable, theme-aligned confirmation dialog for destructive actions in Projects, Configurations, Credentials, and Profile.

The dialog:

- identifies the resource by its human-readable name where available;
- explains that deletion cannot be undone;
- has Cancel and destructive confirmation actions;
- disables dismissal and repeated submission while deletion is running;
- closes on overlay click or Escape when idle;
- restores focus to the trigger after closing.

This replaces immediate deletion and the native `window.confirm`.

### Modal Behavior

Create a shared modal shell or shared modal behavior used by editable dialogs. All modals close consistently through their close button, overlay click, and Escape, unless an operation is in progress.

Project and configuration editors become semantic forms. Credential editing remains a form. Enter submits where normal form semantics allow it, while multiline textareas retain their normal Enter behavior.

### Toasts

Add a lightweight application-level toast provider. Successful create, update, delete, start/restart, and credential validation operations produce concise feedback. Failed operations remain visible as alerts near the relevant page or form rather than disappearing into a toast.

Toasts are announced with an appropriate live region, disappear automatically, and can be dismissed manually.

### Alerts

Add a reusable Alert component with `error`, `warning`, and `info` variants based on the existing dark theme variables. Replace light-theme inline banners in Projects, Project Detail, Credentials, Configurations, and other touched flows.

### Loading State

Extract the polished spinner/panel presentation from ProtectedRoute into a reusable loading component. Pages use this component with page-specific labels instead of raw `Loading...` text.

## Guided Setup

Add a compact three-step setup guide:

1. Add an LLM credential.
2. Create a configuration.
3. Create a project.

For authenticated users, Home loads the user's credentials, configurations, and projects to show completed steps and link the next incomplete step. The guide also appears in empty states on Credentials, Configurations, and Projects with contextual copy and direct links.

Once at least one project exists, the Home guide is hidden. Existing page content remains available regardless of setup progress; the guide informs but does not hard-block navigation.

## Human-Readable Resource Labels

Projects:

- Project cards show a configuration summary such as `gpt-4o - 50 iterations`, resolved from the configurations already loaded on the page.
- The project editor configuration dropdown uses the same summary.
- Project Detail fetches `GET /api/projects/{id}` together with run data and uses the project name as its title.

Configurations:

- Configuration cards use the model name as the primary title and retain `Configuration #<id>` as secondary metadata.
- Linked projects are resolved through `GET /api/projects/me` and displayed by name, with the ID secondary where useful.
- Credentials are displayed as `<Provider> credential #<id>`, for example `OpenAI credential #5`.
- The details modal follows the same naming rules.

IDs remain available as secondary technical identifiers when useful, but not as the main label.

## Navigation and Accessibility

Replace primary navigation Links with NavLinks and add a visible active state using the existing teal primary color and underline treatment. Project detail routes keep Projects active.

The profile menu opens by a button click, exposes `aria-expanded` and `aria-haspopup`, closes on outside click and Escape, and works by keyboard and touch.

The Login/Register cross-links become semantic React Router links rather than clickable spans.

## Language

English is the canonical UI language. Translate the reported Portuguese credential messages, optional labels, Home contact placeholder, generated email body labels, and any other Portuguese user-facing strings encountered in touched files.

Source-code comments do not affect the user-facing language requirement.

## Credential Editing and Errors

Credential edit mode shows only the provider as read-only context and one API key field. It explains: `Enter a new key to replace the current one.` Local model name and port are not displayed as editable because the current backend update contract only accepts a replacement API key.

Create mode continues to show model name and port for local credentials.

Delete and validation failures use the backend response through the central error mapping. The UI must not claim that a credential is linked to a configuration unless the backend returns a specific error conveying that cause. Under the current backend behavior, a foreign-key deletion failure maps to a generic persistence error.

Issue 3 remains excluded: the existing credential-validity initialization behavior is not changed.

## Browser Metadata

Set the static document title to `EvoLab` and replace the default Vite-style favicon with a small EvoLab-specific SVG using the existing dark/teal visual identity.

Add route-aware titles:

- `Home - EvoLab`
- `Projects - EvoLab`
- `<Project name> - EvoLab` when loaded
- `Credentials - EvoLab`
- `Configuration - EvoLab`
- `Profile - EvoLab`
- `Sign In - EvoLab`
- `Register - EvoLab`

## Content Correction

Update Miguel Pinto's GitHub link on Home to:

`https://github.com/MiguelMPinto`

## Testing

Introduce Vitest, React Testing Library, and jsdom because the frontend currently has no automated test runner.

Focused tests cover:

- confirmation requires an explicit second action before deletion;
- Escape and overlay dismissal behavior;
- project form submission through form semantics;
- profile menu click, outside click, and keyboard behavior;
- active navigation state;
- semantic Login/Register links;
- setup guide next-step selection;
- human-readable project, configuration, and credential labels;
- credential edit mode hides local model fields and explains key replacement;
- backend errors are mapped without an invented delete cause;
- route title updates.

Verification includes the focused test suite, the full frontend test suite, ESLint, and the production build.
