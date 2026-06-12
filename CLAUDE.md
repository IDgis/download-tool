# Migration Project Rules

## Boundaries
- NEVER commit to git automatically
- NEVER read .env, .env.*, *.secret, application.conf (prod), or any secrets files
- NEVER run destructive commands (drop, delete, truncate) without explicit confirmation
- Only modify files relevant to the current migration stage

## Current State
- Stage: 0 (not started)
- Play: 2.4.6 | Gradle: 2.12 | Java: 8

## Target Stack
- Gradle: 8.x (intermediate) → 9.x (final)
- Java: 8 (current) → 17 (intermediate) → 21 (final)
- Play: 2.4.6 → 2.5 → 2.6 → 2.7 → 2.8 → 3.0

## Stage Order
1. Gradle 2.12 → 8.x           (Java 8, Play 2.4.6)
2. Play 2.4.6 → 2.5 → 2.6      (Gradle 8, Java 8)
3. Play 2.6 → 2.7 → 2.8        (Gradle 8, Java 8)
4. Java 8 → 11                 (Gradle 8, Play 2.8 — minimum for Play 2.9)
5. Play 2.8 → 2.9              (Gradle 8, Java 11)
6. Java 11 → 17                (Gradle 8, Play 2.9)
7. Gradle 8 → 9                (Java 17, Play 2.9)
8. Play 2.9 → 3.0              (Gradle 9, Java 17)
9. Java 17 → 21

## Build Verification
- Always verify with: `./gradlew build` after changes
- Report errors before proceeding to next step
