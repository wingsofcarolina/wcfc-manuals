#!/usr/bin/env python3
"""
Streamlined Playwright-based authentication flow test for WCFC Manuals
"""

import json
import re
import sys
import time
import requests
import base64
from playwright.sync_api import sync_playwright, expect
import pymongo

def get_verification_code_from_wiremock():
    """Extract verification code from WireMock email requests"""
    print("Checking WireMock for sent emails...")
    
    wiremock_url = "http://localhost:8080"
    
    try:
        response = requests.get(f"{wiremock_url}/__admin/requests")
        if response.status_code != 200:
            raise Exception(f"Failed to get requests from WireMock: {response.text}")
        
        requests_data = response.json()
        
        # Find email send requests
        for request in requests_data.get('requests', []):
            req_data = request.get('request', {})
            url = req_data.get('url', '')
            if '/gmail/v1/users/' in url and '/messages/send' in url:
                try:
                    body = json.loads(req_data.get('body', ''))['raw']
                    decoded_email = base64.urlsafe_b64decode(body + '==').decode('utf-8')
                    
                    code_match = re.search(r'div.{1,6}class.{1,6}code.{1,6}(\d{6}).{1,6}\/div', decoded_email)
                    if code_match:
                        code = code_match.group(1)
                        print(f"Found verification code: {code}")
                        return code

                except Exception as e:
                    print(f"Error parsing email data: {e}")
                    print(f"Request body: {req_data.get('body', '')[:200]}...")
                    continue
        
        print("Email verification code not found")
        return None
    except Exception as e:
        print(f"Error getting verification code from WireMock: {e}")
        return None

def verify_email_sent():
    """Verify that an email was sent through WireMock"""
    wiremock_url = "http://localhost:8080"
    
    try:
        response = requests.get(f"{wiremock_url}/__admin/requests")
        if response.status_code != 200:
            return False
        
        requests_data = response.json()
        
        # Check if there are any Gmail API send requests
        for request in requests_data.get('requests', []):
            req_data = request.get('request', {})
            url = req_data.get('url', '')
            if '/gmail/v1/users/' in url and '/messages/send' in url:
                return True
        
        return False
    except:
        return False

def wait_for_page_ready(page, max_attempts=5):
    """Wait for page to be fully loaded and JavaScript ready"""
    print("Waiting for page to be ready...")
    
    for attempt in range(max_attempts):
        page.wait_for_load_state("load")
        page.wait_for_timeout(2000)  # Wait for JS to initialize
        
        # Check if we have actual content (not just modulepreload)
        content = page.content()
        if "modulepreload" in content and len(content) < 2000:
            print(f"Attempt {attempt + 1}: Page not fully loaded, waiting...")
            page.wait_for_timeout(3000)
            
            # Try to trigger loading
            try:
                page.evaluate("window.scrollTo(0, 100)")
                page.wait_for_timeout(1000)
            except:
                pass
        else:
            print("✓ Page is ready")
            return True
    
    # Last resort: hard refresh
    print("Page still not ready, trying hard refresh...")
    page.reload(wait_until="load")
    page.wait_for_timeout(5000)
    return True

def run_authentication_test():
    """Run the complete authentication flow test"""
    print("Starting streamlined authentication flow test...")
    
    with sync_playwright() as p:
        # Launch browser
        browser = p.chromium.launch(
            headless=True,
            args=[
                '--no-sandbox',
                '--disable-dev-shm-usage',
                '--disable-web-security',
                '--allow-running-insecure-content'
            ]
        )
        
        context = browser.new_context(
            viewport={'width': 1280, 'height': 720},
            java_script_enabled=True,
            ignore_https_errors=True
        )
        
        page = context.new_page()
        
        # Simple console logging
        page.on("console", lambda msg: print(f"Console: {msg.text}") if "404" not in msg.text else None)
        page.on("pageerror", lambda error: print(f"Page error: {error}"))
        
        try:

            # Step 1: Navigate to equipment page (should redirect to login)
            print("Step 1: Navigating to /equipment...")
            
            try:
                response = page.goto("http://localhost:9300/equipment", wait_until="load", timeout=15000)
                print(f"Navigation response: {response.status}")
            except Exception as e:
                print(f"Navigation failed, trying /login directly: {e}")
                response = page.goto("http://localhost:9300/login", wait_until="load", timeout=15000)
            
            # Wait for page to be ready
            wait_for_page_ready(page)
            
            print(f"Current URL: {page.url}")
            
            # Step 2: Fill in email and submit
            print("Step 2: Looking for email input...")
            
            email_input = page.locator('input[type="email"], input[name="email"], input[placeholder*="email" i]')
            expect(email_input).to_be_visible(timeout=10000)
            
            submit_button = page.locator('button[type="submit"], input[type="submit"], button:has-text("Submit"), button:has-text("Login")')
            expect(submit_button).to_be_visible(timeout=5000)
            
            print("✓ Found email input and submit button")
            
            # Clear WireMock requests and fill email
            requests.delete("http://localhost:8080/__admin/requests")
            
            test_email = "test@example.com"
            email_input.clear()
            email_input.fill(test_email)
            
            print(f"Filled email: {email_input.input_value()}")
            
            # Submit form - IMPORTANT: Don't navigate away, stay on same page
            print("Submitting email form...")
            submit_button.click()
            
            # Wait for form processing (but don't expect navigation)
            page.wait_for_timeout(3000)
            
            # Step 3: Verify email was sent
            print("Step 3: Verifying email was sent...")
            
            email_sent = False
            for attempt in range(10):
                if verify_email_sent():
                    email_sent = True
                    break
                time.sleep(1)
            
            if not email_sent:
                raise Exception("Email was not sent through WireMock")
            
            print("✓ Email sent successfully")
            
            # Step 4: Get verification code
            print("Step 4: Getting verification code...")
            
            verification_code = None
            for attempt in range(5):
                verification_code = get_verification_code_from_wiremock()
                if verification_code:
                    break
                time.sleep(1)
            
            if not verification_code:
                raise Exception("Could not get verification code")
            
            print(f"✓ Got verification code: {verification_code}")
            
            # Step 5: Enter verification code
            print("Step 5: Looking for verification code inputs...")
            
            # The page should now show verification code inputs (6 separate fields)
            # Wait for them to appear (page should update without navigation)
            code_inputs = None
            
            for attempt in range(10):
                try:
                    # Look for the multi-input verification code component
                    # The VerificationCode component creates input[type="number"] elements with index attributes
                    code_inputs = page.locator('input[type="number"][index]')
                    if code_inputs.count() >= 6:
                        print(f"✓ Found {code_inputs.count()} verification code input fields")
                        break
                except:
                    pass
                
                print(f"Attempt {attempt + 1}: Waiting for verification inputs to appear...")
                page.wait_for_timeout(1000)
            
            if not code_inputs or code_inputs.count() < 6:
                raise Exception("Could not find verification code input fields (expected 6 separate inputs)")
            
            # Enter verification code into the separate input fields
            print("Entering verification code into separate input fields...")
            
            # Clear all inputs first
            for i in range(6):
                try:
                    input_field = code_inputs.nth(i)
                    input_field.clear()
                except:
                    pass
            
            # Enter each digit into its respective input field
            for i, digit in enumerate(verification_code):
                try:
                    input_field = code_inputs.nth(i)
                    input_field.fill(digit)
                    
                    # Trigger input events to ensure the component processes the input
                    input_field.dispatch_event('input')
                    input_field.dispatch_event('keypress', {'key': digit})
                    
                    # Small delay to allow the component to process
                    page.wait_for_timeout(100)
                except Exception as e:
                    print(f"Error entering digit {digit} at position {i}: {e}")
                    raise Exception(f"Failed to enter verification code digit at position {i}")
            
            print(f"✓ Verification code entered successfully: {verification_code}")
            
            # Find and click verify button
            verify_button = page.locator('input[type="submit"], button[type="submit"], button:has-text("Verify"), button:has-text("Submit"), input[value*="Verify"]')
            expect(verify_button).to_be_visible(timeout=5000)
            
            print("Clicking verify button...")
            verify_button.click()
            
            # Step 6: Wait for authentication to complete
            print("Step 6: Waiting for authentication to complete...")
            
            # Wait for page to load after verification
            page.wait_for_timeout(3000)
            
            # Check if we're redirected to equipment page or if page updates
            current_url = page.url
            print(f"Current URL after verification: {current_url}")
            
            # If still on login page, try manual navigation to equipment
            if "/login" in current_url:
                print("Still on login page, trying to navigate to equipment...")
                try:
                    page.goto("http://localhost:9300/equipment", wait_until="load", timeout=10000)
                except Exception as e:
                    print(f"Manual navigation failed: {e}")
            
            # Wait for equipment page to load
            wait_for_page_ready(page)
            
            # Step 7: Verify we have equipment data
            print("Step 7: Verifying equipment page...")
            
            # Look for equipment-related content
            equipment_indicators = [
                'table',
                'td',
                'tr',
                '.equipment',
                '[data-testid*="equipment"]'
            ]
            
            equipment_found = False
            for selector in equipment_indicators:
                try:
                    elements = page.locator(selector)
                    if elements.count() > 0:
                        equipment_found = True
                        print(f"✓ Found equipment data using selector: {selector}")
                        break
                except:
                    continue
            
            # Also check for equipment names in content
            if not equipment_found:
                content = page.content().lower()
                equipment_names = ['cessna', 'warrior', 'garmin', 'bendix', 'gps', 'navcom', 'transponder', 'mooney']
                for name in equipment_names:
                    if name in content:
                        equipment_found = True
                        print(f"✓ Found equipment data - detected '{name}' in page content")
                        break
            
            if not equipment_found:
                print("❌ Equipment data not found")
                print(f"Page content preview: {page.content()[:1000]}")
                raise Exception("Equipment data not found on the page after authentication")
            
            print("✓ Authentication flow completed successfully!")
            return True
            
        except Exception as e:
            print(f"❌ Test failed: {e}")
            
            # Take screenshot for debugging
            try:
                page.screenshot(path="/tmp/test-failure.png")
                print("Screenshot saved to /tmp/test-failure.png")
            except:
                pass
            
            return False
            
        finally:
            browser.close()

if __name__ == "__main__":
    try:
        success = run_authentication_test()
        if success:
            print("\n🎉 Integration test PASSED!")
            sys.exit(0)
        else:
            print("\n❌ Integration test FAILED!")
            sys.exit(1)
    except Exception as e:
        print(f"\n💥 Test execution error: {e}")
        sys.exit(1)
