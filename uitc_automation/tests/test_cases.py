from config.log_config import log
from playwright.async_api import Page
from models.suite import Case,Module
from runner.actuator import execute


def test_cases(page:Page,caseData:Case,module:Module):
    if not caseData.after or not caseData.before:
        log.debug("前后置为空")
    log.debug(module)