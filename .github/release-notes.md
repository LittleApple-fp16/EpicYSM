Fixes combat animations failing after death and respawn while the YSM model continues using its own walking animation.

- Render ownership belongs to the current player entity and world.
- Respawn and world changes rebuild cached model detection and live skeleton bindings.
- Reload and player cleanup discard stale render ownership.
- Regression tests cover respawn, world replacement, clock rollback, expiration, and cleanup.

Install the JAR for the Minecraft version and loader shown in this release. SHA256SUMS.txt contains the artifact checksum.
