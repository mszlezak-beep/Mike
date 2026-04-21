from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime, timedelta
import json

TIME_FORMAT = "%H:%M"


@dataclass(frozen=True)
class WorkdayConfig:
    start_time: str = "09:00"
    run_minutes: int = 8 * 60
    extension_minutes: int = 0
    page_onload_event: str | None = None
    focus_minutes: int = 50
    short_break_minutes: int = 10
    lunch_minutes: int = 30
    lunch_after_cycles: int = 4

    def validate(self) -> None:
        try:
            datetime.strptime(self.start_time, TIME_FORMAT)
        except ValueError as exc:
            raise ValueError("start_time must use HH:MM 24-hour format") from exc

        positive_values = {
            "run_minutes": self.run_minutes,
            "focus_minutes": self.focus_minutes,
            "short_break_minutes": self.short_break_minutes,
            "lunch_minutes": self.lunch_minutes,
            "lunch_after_cycles": self.lunch_after_cycles,
        }
        for field_name, value in positive_values.items():
            if value <= 0:
                raise ValueError(f"{field_name} must be greater than zero")

        if self.extension_minutes < 0:
            raise ValueError("extension_minutes cannot be negative")
        if self.page_onload_event is not None and not self.page_onload_event.strip():
            raise ValueError("page_onload_event cannot be empty")

    @property
    def total_work_minutes(self) -> int:
        return self.run_minutes + self.extension_minutes


@dataclass(frozen=True)
class WorkdayEvent:
    name: str
    trigger: str
    at_time: str


@dataclass(frozen=True)
class WorkdaySegment:
    label: str
    kind: str
    start_time: str
    end_time: str
    duration_minutes: int


@dataclass(frozen=True)
class WorkdayPlan:
    config: WorkdayConfig
    segments: list[WorkdaySegment]
    end_time: str
    events: list[WorkdayEvent] = field(default_factory=list)

    @property
    def total_work_minutes(self) -> int:
        return sum(segment.duration_minutes for segment in self.segments if segment.kind == "focus")

    @property
    def total_break_minutes(self) -> int:
        return sum(segment.duration_minutes for segment in self.segments if segment.kind != "focus")

    def to_dict(self) -> dict[str, object]:
        return {
            "config": asdict(self.config),
            "end_time": self.end_time,
            "total_work_minutes": self.total_work_minutes,
            "total_break_minutes": self.total_break_minutes,
            "segments": [asdict(segment) for segment in self.segments],
            "events": [asdict(event) for event in self.events],
        }

    def to_json(self) -> str:
        return json.dumps(self.to_dict(), indent=2)


def extend_workday_run(
    config: WorkdayConfig,
    extra_minutes: int,
    *,
    page_onload_event: str | None = None,
) -> WorkdayConfig:
    if extra_minutes <= 0:
        raise ValueError("extra_minutes must be greater than zero")
    config.validate()
    event_name = config.page_onload_event if page_onload_event is None else page_onload_event
    return WorkdayConfig(
        start_time=config.start_time,
        run_minutes=config.run_minutes,
        extension_minutes=config.extension_minutes + extra_minutes,
        page_onload_event=event_name,
        focus_minutes=config.focus_minutes,
        short_break_minutes=config.short_break_minutes,
        lunch_minutes=config.lunch_minutes,
        lunch_after_cycles=config.lunch_after_cycles,
    )


def build_workday_plan(config: WorkdayConfig) -> WorkdayPlan:
    config.validate()

    current = _parse_time(config.start_time)
    remaining_focus_minutes = config.total_work_minutes
    cycles_completed = 0
    lunch_taken = False
    segments: list[WorkdaySegment] = []
    events: list[WorkdayEvent] = []

    if config.page_onload_event:
        events.append(
            WorkdayEvent(
                name=config.page_onload_event,
                trigger="page_onload",
                at_time=config.start_time,
            )
        )

    while remaining_focus_minutes > 0:
        focus_block = min(config.focus_minutes, remaining_focus_minutes)
        segments.append(
            _segment(
                label=f"Focus block {cycles_completed + 1}",
                kind="focus",
                start=current,
                duration_minutes=focus_block,
            )
        )
        current += timedelta(minutes=focus_block)
        remaining_focus_minutes -= focus_block
        cycles_completed += 1

        if remaining_focus_minutes <= 0:
            break

        should_take_lunch = (
            not lunch_taken
            and cycles_completed % config.lunch_after_cycles == 0
            and remaining_focus_minutes > 0
        )
        break_kind = "lunch" if should_take_lunch else "break"
        break_length = config.lunch_minutes if should_take_lunch else config.short_break_minutes
        break_label = "Lunch" if should_take_lunch else f"Break {cycles_completed}"

        segments.append(
            _segment(
                label=break_label,
                kind=break_kind,
                start=current,
                duration_minutes=break_length,
            )
        )
        current += timedelta(minutes=break_length)
        lunch_taken = lunch_taken or should_take_lunch

    return WorkdayPlan(
        config=config,
        segments=segments,
        end_time=current.strftime(TIME_FORMAT),
        events=events,
    )


def render_plan(plan: WorkdayPlan) -> str:
    lines = [
        "Workday plan",
        f"Start: {plan.config.start_time}",
        f"End: {plan.end_time}",
        f"Work minutes: {plan.total_work_minutes}",
        f"Break minutes: {plan.total_break_minutes}",
        "",
    ]

    if plan.events:
        lines.append("Events:")
        for event in plan.events:
            lines.append(f"- {event.trigger} @ {event.at_time} | {event.name}")
        lines.append("")

    lines.append("Schedule:")

    for segment in plan.segments:
        lines.append(
            f"- {segment.start_time}-{segment.end_time} | {segment.label} "
            f"({segment.kind}, {segment.duration_minutes}m)"
        )

    return "\n".join(lines)


def _parse_time(value: str) -> datetime:
    return datetime.strptime(value, TIME_FORMAT)


def _segment(
    *,
    label: str,
    kind: str,
    start: datetime,
    duration_minutes: int,
) -> WorkdaySegment:
    end = start + timedelta(minutes=duration_minutes)
    return WorkdaySegment(
        label=label,
        kind=kind,
        start_time=start.strftime(TIME_FORMAT),
        end_time=end.strftime(TIME_FORMAT),
        duration_minutes=duration_minutes,
    )
