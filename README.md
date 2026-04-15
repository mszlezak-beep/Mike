# Mike

Invoke-based orchestration for planning and extending a workday run.

## Quick start

Install the project dependencies and then run one of the invoke tasks:

```bash
python -m pip install -e ".[dev]"
inv workday.run
inv workday.extend --minutes 45
```

## Available tasks

- `inv workday.run`: build a workday plan from a start time and run length
- `inv workday.extend`: extend the base workday by additional minutes

Both tasks print a human-readable schedule by default and can emit JSON with
`--json-output`.
