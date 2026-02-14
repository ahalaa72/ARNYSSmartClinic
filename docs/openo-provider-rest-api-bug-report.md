# Open-O REST API Bug Report: Provider List Endpoint

**Date:** February 14, 2026
**Reporter:** ARNYS Reservation System Integration Team
**Severity:** Medium
**Component:** REST API - Provider Service

---

## Summary

The REST API endpoint `GET /ws/services/providerService/providers` returns HTTP 500 Internal Server Error due to a JAXB serialization issue with the `ProviderTransfer` class.

---

## Environment

- **Open-O Version:** 1.x (tested on local development instance)
- **Java Version:** OpenJDK 8
- **Application Server:** Tomcat (embedded in Oscar)
- **Database:** MariaDB 10.x
- **OS:** macOS / Linux

---

## Steps to Reproduce

### 1. Configure OAuth 1.0a Authentication

Ensure you have valid OAuth credentials configured for the REST API.

### 2. Make the API Request

```bash
# Using curl with OAuth 1.0a signature
curl -X GET "http://localhost:8080/oscar/ws/services/providerService/providers" \
  -H "Accept: application/json" \
  -H "Authorization: OAuth oauth_consumer_key=\"YOUR_KEY\", oauth_token=\"YOUR_TOKEN\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"...\", oauth_nonce=\"...\", oauth_version=\"1.0\", oauth_signature=\"...\""
```

### 3. Observe the Error

**Expected Result:** JSON array of provider objects
**Actual Result:** HTTP 500 Internal Server Error

---

## Error Details

### Server Log Output

```
javax.xml.bind.JAXBException: class ca.openosp.openo.webserv.transfer_objects.ProviderTransfer
nor any of its super class is known to this context.

    at com.sun.xml.bind.v2.runtime.JAXBContextImpl.getBeanInfo(JAXBContextImpl.java:519)
    at com.sun.xml.bind.v2.runtime.XMLSerializer.childAsRoot(XMLSerializer.java:482)
    ...
```

### HTTP Response

```
HTTP/1.1 500 Internal Server Error
Content-Type: text/html;charset=utf-8

<html>
<head><title>500 Internal Server Error</title></head>
<body>
<h1>HTTP Status 500 – Internal Server Error</h1>
</body>
</html>
```

---

## Root Cause Analysis

The `ProviderTransfer` class is missing the `@XmlRootElement` annotation required for JAXB to serialize the object to XML/JSON for REST API responses.

When the REST framework attempts to serialize the list of `ProviderTransfer` objects, JAXB throws an exception because it doesn't recognize the class as a valid XML element.

### Comparison with Working Endpoint

The `DemographicTransfer` class (used by `/demographics` endpoint) works correctly because it HAS the annotation:

```java
// DemographicTransfer.java - WORKS
@XmlRootElement
public class DemographicTransfer {
    // ...
}
```

The `ProviderTransfer` class is MISSING this annotation:

```java
// ProviderTransfer.java - BROKEN
public class ProviderTransfer {  // Missing @XmlRootElement
    // ...
}
```

---

## Proposed Fix

### File to Modify

```
src/main/java/ca/openosp/openo/webserv/transfer_objects/ProviderTransfer.java
```

Or in older Oscar versions:
```
src/main/java/org/oscarehr/ws/rest/transfer/ProviderTransfer.java
```

### Code Change

Add the `@XmlRootElement` annotation to the `ProviderTransfer` class:

```java
package ca.openosp.openo.webserv.transfer_objects;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlRootElement  // <-- ADD THIS ANNOTATION
@XmlAccessorType(XmlAccessType.FIELD)  // Optional but recommended
public class ProviderTransfer {

    private String providerNo;
    private String firstName;
    private String lastName;
    private String specialty;
    private String status;
    // ... other fields

    // Getters and setters...
}
```

### Required Import

Make sure the import is present:

```java
import javax.xml.bind.annotation.XmlRootElement;
```

---

## Alternative Fix (If Using Jersey/JAX-RS)

If using Jersey with JSON serialization, you may also need to ensure the class is registered with the JSON provider:

```java
@XmlRootElement(name = "provider")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProviderTransfer implements Serializable {
    // ...
}
```

And if returning a list, wrap it in a container class:

```java
@XmlRootElement(name = "providers")
public class ProviderListTransfer {
    @XmlElement(name = "provider")
    private List<ProviderTransfer> providers;

    // Constructor, getters, setters
}
```

---

## Testing the Fix

### 1. Rebuild the Application

```bash
# From Oscar/Open-O root directory
mvn clean package -DskipTests

# Or with Gradle if applicable
./gradlew clean build -x test
```

### 2. Deploy to Tomcat

```bash
# Stop Tomcat
./catalina.sh stop

# Copy new WAR file
cp target/oscar.war $CATALINA_HOME/webapps/

# Start Tomcat
./catalina.sh start
```

### 3. Test with curl

```bash
# Test provider list endpoint
curl -X GET "http://localhost:8080/oscar/ws/services/providerService/providers" \
  -H "Accept: application/json" \
  -H "Authorization: OAuth ..." \
  -v

# Expected response (HTTP 200):
# [
#   {
#     "providerNo": "999998",
#     "firstName": "John",
#     "lastName": "Doctor",
#     "specialty": "Family Medicine",
#     "status": "1"
#   },
#   ...
# ]
```

### 4. Test with Python

```python
#!/usr/bin/env python3
"""Test script for provider REST API endpoint"""

import requests
from requests_oauthlib import OAuth1

# OAuth credentials
oauth = OAuth1(
    client_key='YOUR_CONSUMER_KEY',
    client_secret='YOUR_CONSUMER_SECRET',
    resource_owner_key='YOUR_ACCESS_TOKEN',
    resource_owner_secret='YOUR_TOKEN_SECRET'
)

# Make request
url = 'http://localhost:8080/oscar/ws/services/providerService/providers'
response = requests.get(url, auth=oauth, headers={'Accept': 'application/json'})

print(f"Status: {response.status_code}")
print(f"Response: {response.text}")

# Verify response
if response.status_code == 200:
    providers = response.json()
    print(f"SUCCESS: Found {len(providers)} providers")
    for p in providers[:3]:  # Show first 3
        print(f"  - {p.get('providerNo')}: {p.get('firstName')} {p.get('lastName')}")
else:
    print(f"FAILED: {response.status_code}")
```

### 5. Automated Test (JUnit)

Add this test to the test suite:

```java
package ca.openosp.openo.webserv;

import static org.junit.Assert.*;
import org.junit.Test;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import ca.openosp.openo.webserv.transfer_objects.ProviderTransfer;

public class ProviderTransferJaxbTest {

    @Test
    public void testProviderTransferCanBeSerialized() throws Exception {
        // Create test provider
        ProviderTransfer provider = new ProviderTransfer();
        provider.setProviderNo("999998");
        provider.setFirstName("Test");
        provider.setLastName("Doctor");

        // Verify JAXB can serialize it
        JAXBContext context = JAXBContext.newInstance(ProviderTransfer.class);
        Marshaller marshaller = context.createMarshaller();

        StringWriter writer = new StringWriter();
        marshaller.marshal(provider, writer);

        String xml = writer.toString();
        assertNotNull(xml);
        assertTrue(xml.contains("Test"));
        assertTrue(xml.contains("Doctor"));
    }

    @Test
    public void testProviderTransferListCanBeSerialized() throws Exception {
        // Test list serialization if using wrapper class
        // ...
    }
}
```

---

## Other Endpoints That May Have Same Issue

Please verify these endpoints also have `@XmlRootElement` on their transfer objects:

| Endpoint | Transfer Class | Status |
|----------|---------------|--------|
| `/providerService/providers` | `ProviderTransfer` | ❌ BROKEN |
| `/demographics` | `DemographicTransfer` | ✅ Works |
| `/schedule/add` | `AppointmentTransfer` | ✅ Works |
| `/schedule/{provider}/day/{date}` | `ScheduleTransfer` | ✅ Works |
| `/schedule/statuses` | `StatusTransfer` | ✅ Works |

---

## Workaround (Until Fix is Deployed)

External applications can use the SOAP API endpoint instead:

```python
# SOAP API works correctly for provider list
from zeep import Client
from zeep.wsse.username import UsernameToken

client = Client(
    'http://localhost:8080/oscar/ws/ProviderService?wsdl',
    wsse=UsernameToken('username', 'password')
)

providers = client.service.getProviders(active=True)
```

---

## Impact

- **Affected Users:** Any external application using OAuth REST API to retrieve provider list
- **Affected Functionality:** Cannot retrieve provider list via REST API
- **Workaround Available:** Yes (use SOAP API)

---

## References

- [JAXB @XmlRootElement Documentation](https://docs.oracle.com/javaee/7/api/javax/xml/bind/annotation/XmlRootElement.html)
- [JAX-RS/Jersey JSON Serialization](https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/media.html)
- [Oscar EMR REST API Documentation](https://oscar-emr.com/documentation)

---

## Contact

If you need additional information or assistance testing, please contact:
- **Integration Team:** ARNYS Reservation System
- **Issue Discovered During:** OAuth REST API integration for cloud deployment
