from pydantic import BaseModel
from typing import Optional

class Step(BaseModel):
    seq: int = 0
    description: str = ""
    action: str = ""
    target: str = ""
    value: str = ""
    wait: Optional[str] = None

class Execute(BaseModel):
    action: str = ""
    target: str = ""
    value: str = ""

class Assertion(BaseModel):
    target: str
    matcher: str = "visible"
    expected: str = ""

class Case(BaseModel):
    id: str = ""
    title: str = ""
    before: list[Execute] = []
    after: list[Execute] = []
    steps: list[Step] = []
    assertions: list[Assertion] = []

class Module(BaseModel):
    name: str
    url: str

class TestSuite(BaseModel):
    module: Module
    cases: list[Case]