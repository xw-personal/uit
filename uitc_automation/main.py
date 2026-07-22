from runner.loader import load_suite


if __name__ == "__main__":
    result = load_suite("I:\\Project\\ui_test\\uitc_automation\\data\\case.yaml")
    print(result.model_dump_json())