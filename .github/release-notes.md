Restores the caller’s pose stack after OpenYSM and Yes Steve Model redraws, delegated player renderers, and Epic Fight held-item layers. Also handles renderers that leave extra frames on a normal return, which can otherwise cause the world renderer to stop with `Pose stack not empty`.

The runtime diagnostics record the affected route and renderer when they detect an unbalanced stack. Use `/epicysm report` to export a diagnostic ZIP under `logs/epicysm-diagnostics`.
