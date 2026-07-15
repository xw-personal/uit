from dataclasses import dataclass,field

@dataclass
class DataItem:
    value: str = ""
    expected: str = ""

@dataclass
class DataGroup:
    effective: list[DataItem] = field(default_factory=list)
    invalid: list[DataItem] = field(default_factory=list)
    boundary: list[DataItem] = field(default_factory=list)

@dataclass
class Step:
    no: int
    description: str = ""
    action: str = ""
    target: str = ""
    value: str = ""
    wait: str = ""

@dataclass
class Assertion:
    target: str
    matcher: str
    expected: str = ""

@dataclass
class TestCase:
    id: str
    title: str = ""
    type: str = ""
    priority: str = ""
    depends_on: list[str] = field(default_factory=list)
    precondition: str = ""
    dataGroup: str = ""
    steps: list[Step] = field(default_factory=list)
    assertions: list[Assertion] = field(default_factory=list)

@dataclass
class Module:
    name: str = ""
    url: str = ""

@dataclass
class TestSuite:
    module: Module
    dataLibrary: dict[str, DataGroup]
    cases: list[TestCase]