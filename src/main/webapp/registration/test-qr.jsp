<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="ca.openosp.openo.utility.SpringUtils" %>
<%@ page import="ca.openosp.openo.registration.util.QRCodeGenerator" %>
<%
    String result = "";
    String error = "";
    String qrBase64 = "";

    try {
        // Try to get the bean
        QRCodeGenerator qrGen = SpringUtils.getBean(QRCodeGenerator.class);

        if (qrGen == null) {
            error = "QRCodeGenerator bean is NULL - not found in Spring context";
        } else {
            result = "QRCodeGenerator bean loaded successfully!";

            // Try to generate a QR code
            try {
                qrBase64 = qrGen.generateQRCodeBase64("https://test.com/registration", 200, 200);
                result += " QR Code generated successfully!";
            } catch (Exception e) {
                error = "QR generation failed: " + e.getClass().getName() + " - " + e.getMessage();
                e.printStackTrace();
            }
        }
    } catch (Exception e) {
        error = "Error loading QRCodeGenerator: " + e.getClass().getName() + " - " + e.getMessage();
        e.printStackTrace();
    }
%>
<!DOCTYPE html>
<html>
<head><title>QR Test</title></head>
<body>
<h1>QR Code Generation Test</h1>

<% if (!result.isEmpty()) { %>
<p style="color: green;"><strong>SUCCESS:</strong> <%= result %></p>
<% } %>

<% if (!error.isEmpty()) { %>
<p style="color: red;"><strong>ERROR:</strong> <%= error %></p>
<% } %>

<% if (!qrBase64.isEmpty()) { %>
<h2>Generated QR Code:</h2>
<img src="data:image/png;base64,<%= qrBase64 %>" alt="Test QR Code" style="border: 2px solid black;">
<% } %>

<h3>Debug Info:</h3>
<ul>
<li>QR Base64 length: <%= qrBase64.length() %></li>
<li>Spring context available: <%= (SpringUtils.getBean(QRCodeGenerator.class) != null) %></li>
</ul>
</body>
</html>
