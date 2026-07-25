# CALC

**Precision Engine.**

The first app under [NeonCoreLabs](https://github.com/neoncorelabs), built on
[`neon-core`](https://github.com/neoncorelabs/neon-core).

## Status

Specced, not yet built. All architecture decisions are final for v1 — see
docs below. Code in `app/` is currently an empty package scaffold matching
`CALC-ARCH-01`'s module structure.

## Docs

Full specifications live in
[`neoncorelabs/docs/calc`](https://github.com/neoncorelabs/docs/tree/main/calc) —
this repo doesn't own its own documentation (see
[`docs/core/PRINCIPLES.md`](https://github.com/neoncorelabs/docs/blob/main/core/PRINCIPLES.md)).

| Doc | Covers |
|---|---|
| `CALC-ARCH-01` | MVVM, single-module structure, latency tiers, DI |
| `CALC-UI-01` | Screens, gestures, states |
| `CALC-ENGINE-01` | Expression grammar, precision, error handling |
| `CALC-DATA-01` | Room + DataStore persistence |
| `CALC-CURRENCY-01` | Currency conversion (conditional feature) |

## Structure

Single Gradle module, package-separated per `CALC-ARCH-01` — no
`feature/`/`domain`/`data` multi-module split; that's a deliberate choice,
not an oversight (see `PRINCIPLES.md`: structure follows demonstrated
complexity).

```
calc/
└── app/src/main/kotlin/calc/
    ├── ui/          Compose screens and components
    ├── viewmodel/   MVVM view models
    ├── engine/      expression parsing/evaluation — pure Kotlin, no Android deps
    ├── model/       data classes
    ├── storage/     Room + DataStore
    └── settings/    settings screen + persisted preferences
```

## Depends on

`neon-core`, via `includeBuild("../neon-core")`. Pinned to a tagged
release, not a moving branch.
