# Dedicated-server resource regression

Use Java 25, Gradle 9.2.1, Minecraft 26.1.2 and NeoForge 126. This add-on belongs only on a disposable test server, never a live server.

1. Place the tested mod and dependencies in this project's ignored `mods/` directory. HNN tests also need Twilight Forest.
2. Run `gradle -p tests/server-regression jar` from the repository root.
3. Put the resulting test JAR in a disposable server with the same mods, set `server-ip=127.0.0.1`, start with no players, and run `serverregression` from its console.
4. Every named case must pass. Stop and discard the fixture afterward.

HNN tests decode the seven actual resource recipes using the native recipe codec and check the goblin data model registry. Replication tests supply one populated-tag and one absent-tag control recipe; the populated one must load and the empty one must be skipped before ingredient decoding. No production recipes, player data or graphical client are involved.
