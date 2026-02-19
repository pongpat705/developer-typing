import time
from playwright.sync_api import sync_playwright

def verify_theme(page):
    print("Navigating to http://localhost:5173")
    try:
        page.goto("http://localhost:5173", timeout=30000)
    except Exception as e:
        print(f"Error navigating: {e}")
        return

    # Wait for the game container
    print("Waiting for .game-container")
    try:
        page.wait_for_selector(".game-container", timeout=10000)
    except Exception as e:
        print(f"Error waiting for selector: {e}")
        # Take screenshot anyway to see what happened
        page.screenshot(path="/tmp/error_state.png")
        return

    # 1. Verify Light Theme (Default)
    body_class = page.evaluate("document.body.className")
    print(f"Initial Body Class: '{body_class}'")

    # Check computed background color of body
    bg_color = page.evaluate("window.getComputedStyle(document.body).backgroundColor")
    print(f"Initial Body Background Color: {bg_color}")

    # Take screenshot of Light Theme
    page.screenshot(path="/tmp/light_theme.png")
    print("Screenshot taken: /tmp/light_theme.png")

    # 2. Toggle Theme
    print("Clicking toggle button")
    toggle_btn = page.locator(".theme-toggle")
    if toggle_btn.count() == 0:
        print("Toggle button not found!")
    else:
        toggle_btn.click()

    # Wait a bit for transition
    time.sleep(1)

    # 3. Verify Dark Theme
    body_class_after = page.evaluate("document.body.className")
    print(f"Body Class after toggle: '{body_class_after}'")

    bg_color_dark = page.evaluate("window.getComputedStyle(document.body).backgroundColor")
    print(f"Body Background Color after toggle: {bg_color_dark}")

    if "dark-theme" in body_class_after:
        print("Success: 'dark-theme' class added.")
    else:
        print("Failure: 'dark-theme' class NOT added.")

    # Take screenshot of Dark Theme
    page.screenshot(path="/tmp/dark_theme.png")
    print("Screenshot taken: /tmp/dark_theme.png")

    # Toggle back to verify
    print("Clicking toggle button again")
    toggle_btn.click()
    time.sleep(0.5)
    body_class_final = page.evaluate("document.body.className")
    if "dark-theme" not in body_class_final:
         print("Success: 'dark-theme' class removed.")
    else:
         print("Failure: 'dark-theme' class NOT removed.")

if __name__ == "__main__":
    with sync_playwright() as p:
        print("Launching browser")
        browser = p.chromium.launch()
        page = browser.new_page()
        verify_theme(page)
        browser.close()
