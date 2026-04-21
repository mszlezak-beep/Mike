from workday.orchestration import (
    WorkdayConfig,
    WorkdayEvent,
    build_workday_plan,
    extend_workday_run,
    render_plan,
)


def test_build_workday_plan_inserts_lunch_after_configured_cycles() -> None:
    config = WorkdayConfig(
        start_time="09:00",
        run_minutes=250,
        focus_minutes=50,
        short_break_minutes=10,
        lunch_minutes=30,
        lunch_after_cycles=4,
    )

    plan = build_workday_plan(config)

    assert [segment.kind for segment in plan.segments] == [
        "focus",
        "break",
        "focus",
        "break",
        "focus",
        "break",
        "focus",
        "lunch",
        "focus",
    ]
    assert plan.end_time == "14:10"
    assert plan.total_work_minutes == 250
    assert plan.total_break_minutes == 60


def test_extend_workday_run_adds_minutes_to_plan() -> None:
    base_config = WorkdayConfig(run_minutes=120, focus_minutes=60, short_break_minutes=15)

    base_plan = build_workday_plan(base_config)
    extended_plan = build_workday_plan(extend_workday_run(base_config, 30))

    assert base_plan.end_time == "11:15"
    assert extended_plan.end_time == "12:00"
    assert extended_plan.total_work_minutes == 150


def test_extend_workday_run_accepts_page_onload_event() -> None:
    base_config = WorkdayConfig(run_minutes=120, focus_minutes=60, short_break_minutes=15)

    extended_plan = build_workday_plan(
        extend_workday_run(base_config, 30, page_onload_event="workday.extend.onload")
    )

    assert extended_plan.events == [
        WorkdayEvent(name="workday.extend.onload", trigger="page_onload", at_time="09:00")
    ]
    assert "page_onload @ 09:00 | workday.extend.onload" in render_plan(extended_plan)


def test_validate_rejects_blank_page_onload_event() -> None:
    config = WorkdayConfig(page_onload_event="   ")

    try:
        config.validate()
    except ValueError as exc:
        assert str(exc) == "page_onload_event cannot be empty"
    else:
        raise AssertionError("Expected ValueError for blank page_onload_event")


def test_render_plan_outputs_readable_schedule() -> None:
    plan = build_workday_plan(WorkdayConfig(run_minutes=60, focus_minutes=30, short_break_minutes=5))

    rendered = render_plan(plan)

    assert "Workday plan" in rendered
    assert "Schedule:" in rendered
    assert "Focus block 1" in rendered
    assert "09:35-10:05" in rendered
