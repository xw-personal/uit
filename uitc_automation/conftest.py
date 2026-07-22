from pathlib import Path
from runner.loader import load_suite
from runner.actuator import execute
from models.suite import Step
from models.suite import TestSuite
from playwright.sync_api import sync_playwright,Browser
from config.log_config import log
import pytest

dataDir:Path = Path(__file__).resolve().parent / "data"
yamlFiles:list = list(dataDir.glob("test*.yaml"))
loginFile = dataDir / "login.yaml"

def pytest_generate_tests(metafunc):
    if "caseData" in metafunc.fixturenames:
        for yamlFile in yamlFiles:
            params = []
            testSuite:TestSuite = load_suite(yamlFile)
            for case in testSuite.cases:
                params.append(pytest.param(case,id=case.id))
            metafunc.parametrize("caseData",params)
            metafunc.parametrize("module",[testSuite.module])

@pytest.fixture(scope="session")
def browser():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        yield browser
        browser.close()

@pytest.fixture(scope="session")
def page(browser:Browser):
    context = browser.new_context(ignore_https_errors=True)
    page = context.new_page()
    # 先按照login.yaml进行登录
    loginSuite = load_suite(loginFile)
    log.info("开始登录")
    if loginSuite.module.url:
        page.goto(loginSuite.module.url)
        log.info(f"跳转到:{loginSuite.module.url}")
        page.wait_for_load_state("domcontentloaded")
    for step in loginSuite.cases[0].steps:
        execute(step,page)