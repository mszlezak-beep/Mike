from __future__ import annotations

from invoke import Collection, task

from workday.orchestration import WorkdayConfig, build_workday_plan, extend_workday_run, render_plan


def _config_from_inputs(
    *,
    start: str,
    hours: float,
    focus: int,
    short_break: int,
    lunch: int,
    lunch_after: int,
) -> WorkdayConfig:
    run_minutes = int(hours * 60)
    return WorkdayConfig(
        start_time=start,
        run_minutes=run_minutes,
        focus_minutes=focus,
        short_break_minutes=short_break,
        lunch_minutes=lunch,
        lunch_after_cycles=lunch_after,
    )


@task(
    help={
        "start": "Start time in HH:MM format",
        "hours": "Base run length in hours",
        "focus": "Length of each focus block in minutes",
        "short_break": "Break length between focus blocks",
        "lunch": "Lunch break length in minutes",
        "lunch_after": "Insert lunch after this many focus blocks",
        "json_output": "Emit machine-readable JSON instead of text",
    }
)
def run(
    _ctx,
    start: str = "09:00",
    hours: float = 8,
    focus: int = 50,
    short_break: int = 10,
    lunch: int = 30,
    lunch_after: int = 4,
    json_output: bool = False,
) -> None:
    """Build and print a workday run plan."""
    config = _config_from_inputs(
        start=start,
        hours=hours,
        focus=focus,
        short_break=short_break,
        lunch=lunch,
        lunch_after=lunch_after,
    )
    plan = build_workday_plan(config)
    print(plan.to_json() if json_output else render_plan(plan))


@task(
    help={
        "start": "Start time in HH:MM format",
        "hours": "Base run length in hours before the extension",
        "minutes": "Additional work minutes to append to the run",
        "page_onload_event": "Event name emitted at page onload in the extended plan",
        "focus": "Length of each focus block in minutes",
        "short_break": "Break length between focus blocks",
        "lunch": "Lunch break length in minutes",
        "lunch_after": "Insert lunch after this many focus blocks",
        "json_output": "Emit machine-readable JSON instead of text",
    }
)
def extend(
    _ctx,
    start: str = "09:00",
    hours: float = 8,
    minutes: int = 30,
    page_onload_event: str = "",
    focus: int = 50,
    short_break: int = 10,
    lunch: int = 30,
    lunch_after: int = 4,
    json_output: bool = False,
) -> None:
    """Extend the base workday run and print the updated plan."""
    config = _config_from_inputs(
        start=start,
        hours=hours,
        focus=focus,
        short_break=short_break,
        lunch=lunch,
        lunch_after=lunch_after,
    )
    event_name = page_onload_event or None
    extended_config = extend_workday_run(config, minutes, page_onload_event=event_name)
    plan = build_workday_plan(extended_config)
    print(plan.to_json() if json_output else render_plan(plan))


ns = Collection()
workday = Collection("workday")
workday.add_task(run)
workday.add_task(extend)
ns.add_collection(workday)
