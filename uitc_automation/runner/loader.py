import yaml
from models.suite import TestSuite, Module, DataGroup, DataItem, TestCase, Step, Assertion

def load_suite(path: str) -> TestSuite:
    raw = yaml.safe_load(open(path,encoding="utf-8"))
    m = raw["module"]
    module: Module = Module(name=m["name"],url=["url"])
