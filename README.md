# ZaloPay Spring V1

A Spring Boot application for integrating ZaloPay payment functionality.

---

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Key Components](#key-components)
- [Technical Stack](#technical-stack)

---

## Overview

This project provides a secure and modular implementation for ZaloPay payment integration using Java and Spring Boot.

---

## Project Structure

```text
src/ 
└── main/ 
├── java/ 
│ └── com.vn.zalopay/ 
│ ├── ZaloPaySpringV1Application.java 
│ ├── config/ 
│ │ └── ZaloPayConfig.java 
│ ├── controller/ 
│ │ └── ZaloPayController.java 
│ ├── service/ 
│ │ └── ZaloPayService.java 
│ └── crypto/ 
│ ├── HMACUtil.java 
│ ├── HexStringUtil.java 
│ └── RSAUtil.java 
│ └── util/ 
│ └── ZaloPayUtil.java 
└── resources/ 
    └── application.yaml
```
---

## Key Components

- **Main Application**
  - `ZaloPaySpringV1Application.java`: Entry point for the Spring Boot application.

- **Configuration**
  - `ZaloPayConfig.java`: Configuration for ZaloPay settings.

- **Controller**
  - `ZaloPayController.java`: Handles HTTP requests for ZaloPay operations.

- **Service**
  - `ZaloPayService.java`: Business logic for ZaloPay integration.

- **Utilities**
  - `HMACUtil.java`: HMAC cryptographic operations.
  - `HexStringUtil.java`: Hex string utilities.
  - `RSAUtil.java`: RSA encryption/decryption.
  - `ZaloPayUtil.java`: General ZaloPay utility functions.

---

## Technical Stack

- Java 17
- Spring Boot
- Maven
- Lombok

---

## Configuration

Application settings are managed in `application.yaml`.

---

## Notes

- Follows standard Maven and Spring Boot project conventions.
- Clear separation of concerns between controllers, services, and utilities.
- Crypto utilities ensure secure payment processing.

---
