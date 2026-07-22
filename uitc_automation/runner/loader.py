import yaml
from models.suite import TestSuite, Module, Case

"""
读取@parm:path中的内容并解析成用例列表
"""
def load_suite(path: str) -> TestSuite:
    raw = yaml.safe_load(open(path,encoding="utf-8"))
    # 读取单个yaml文件的module和页面url
    module = Module(**raw["module"])
    cases:list = raw["cases"]
    testCases:list[Case] = []
    for case in cases:
        dataSets:list = case.pop("dataSet",[])
        for i, dataSet in enumerate(dataSets):
            testCase = Case(**case)
            testCase.id = dataSet.get("test_id")
            testCase.title += f"-{dataSet.get('description')}"
            for step in testCase.steps:
                   step.value = replace_value(step.value,dataSet)
            for assertion in testCase.assertions:
                assertion.expected = replace_value(assertion.expected,dataSet)
            
            testCases.append(testCase)
    return TestSuite(module=module,cases=testCases)

def replace_value(value:str,dataSet:dict):
    import re
    """
    将字符串中的 ${变量名} 替换为 dataSet 中的值。
    如果变量名不存在，保持原样。
    """
    def replacer(match):
        key = match.group(1)
        return str(dataSet.get(key, match.group(0)))  # 找不到就保留 ${...}
    return re.sub(r'\$\{(\w+)\}', replacer, value)
    # for item in raw.get("cases",[]):

    #     testCast = TestCase(item["id"],title=["title"],type=["type"],priority=["priority"],
    #              depends_on=item.get("depends_on",[]),precondition=item["precondition"],
    #              dataGroup=item["dataGroup"],
    #              steps=[Step(**i) for i in item.get("steps",[])],
    #              assertions=[Assertion(**i) for i in item.get("assertions",[])])
    #     cases.append(testCast)
    # for item in raw.get("cases",[]):
    #     groupName = item["dataGroup"]
    #     dgroup = dataLibrary.get(groupName,)
    #     i = 0
    #     for k,vs in dgroup.items():
    #         for v in vs:
    #             id = f"{groupName}_{i}"
    #             steps = [Step(**i) for i in item.get("steps",[])]
    #             for step in steps:
    #                 step.value 
    #             testCast = TestCase(id,title=item["title"],type=item["type"],priority=item["priority"],
    #                     depends_on=item.get("depends_on",[]),precondition=item["precondition"],
    #                     dataGroup=item["dataGroup"],
    #                     steps=[Step(**i) for i in item.get("steps",[])],
    #                     assertions=[Assertion(**i) for i in item.get("assertions",[])])
    return TestSuite(module=module,dataLibrary=dataLibrary,cases=cases)