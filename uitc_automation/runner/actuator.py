from models.suite import Step
from playwright.sync_api import Page
from config.log_config import log

def execute(step:Step,page:Page):
    if step.action == "fill":
        log.info(step.description)
        page.fill(step.target,step.value)
    if step.action == "click":
        log.info(step.description)
        page.click(step.target)
    if step.action == "select":
        # 先判断是否为原生 select
        log.info(step.description)
        element = page.locator(step.target)
        if element.evaluate("el => el.tagName") == "SELECT":
            page.select_option(step.target, value=step.value)
        else:
            # 模拟下拉：点击触发器 → 点击选项
            page.click(step.target)  # 点击按钮
            # 假设选项通过 step.value 指定文本
            page.click(f".ant-dropdown-menu-item:has-text('{step.value}')")