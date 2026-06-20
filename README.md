MotorPH Payroll Management System

A modern, desktop-based payroll management system built with Java Swing. MotorPH automates the calculation of employee salaries, including accurate Philippine statutory deductions (SSS, PhilHealth, Pag-IBIG, and Withholding Tax)

Features
- Secure Login System with authentication.
- Modern Dark Mode UI for a professional and eye-friendly experience.
- Automated Payroll Calculations:
  - Gross Pay based on actual hours worked.
  - Prorated deductions based on attendance.
- Philippine Statutory Deductions (Fully Compliant):
  - SSS (Social Security System) - Tiered contribution table.
  - PhilHealth - 3% total premium (50/50 employer-employee share).
  - Pag-IBIG (HDMF) - 2% contribution (capped at P100).
  - Withholding Tax - Compliant with the TRAIN Law (RA 10963) tax brackets.
- Smart Time Tracking:
  - 10-minute grace period for tardiness.
  - Automatic 1-hour unpaid lunch break deduction (12:00 PM - 1:00 PM).
- Monthly Payroll Filtering: View payroll data by specific months or all months combined.
- CSV Integration: Import, export, and backup employee and attendance data.
- Employee Management: Add, edit, delete, and search employee records.

 Tech Stack
- Language: Java (JDK 11 or higher recommended)
- GUI Framework: Java Swing & AWT
- Data Handling: Java NIO & File I/O (CSV parsing)

How to Run

Prerequisites
- Install [Java Development Kit (JDK) 11+](https://adoptium.net/)
- An IDE like IntelliJ IDEA, Eclipse, or NetBeans (optional but recommended).

Installation
1. Clone the repository:
   ```bash

2. Open the project in your preferred Java IDE.
3. Ensure employees.csv and attendance.csv are in the root directory of the project.
4. Run the MotorPHGUI.java file.

Default Login Credentials
When you launch the application, use the following credentials to log in:
Username: payroll_staff
Password: 12345

CSV Data Formats
For the system to read your data correctly, ensure your CSV files follow these exact formats:
1. employees.csv
Contains the master list of employees and their salary rates.
Employee #, Last Name, First Name, Birthday, Status, Position, Immediate Supervisor, Basic Salary, Gross Semi-monthly Rate, Hourly Rate
10001, Garcia, Manuel III,10/11/1983, Regular, Chief Executive Officer, N/A,90000.00,45000.00,535.71
10002, Lim, Antonio,06/19/1988, Regular, Chief Operating Officer, Garcia, Manuel III,60000.00,30000.00,357.14

2. attendance.csv
Contains the daily time records. The system reads columns 0, 3, 4, and 5.
Employee #, Last Name, First Name, Date, Log In, Log Out
10001,Garcia,Manuel III,06/03/2024,8:59,18:31
10002, Lim, Antonio,06/03/2024,10:35,19:44

Screemshots

Login screen:

<img width="463" height="532" alt="image" src="https://github.com/user-attachments/assets/08fe7aca-cbbd-422d-a851-efcaa91c6cf6" />


Main Dashboard:

<img width="1385" height="739" alt="image" src="https://github.com/user-attachments/assets/cc706a70-0020-4af1-85b9-8e96c7c1de1d" />

Payroll Report:

<img width="1385" height="728" alt="image" src="https://github.com/user-attachments/assets/91a34ac6-b4d9-44cc-a63d-5067a912545d" />


Project Structure
MotorPH-Payroll-System/
├── src/
│   └── MotorPHGUI.java       # Main application logic and UI
── employees.csv             # Employee master data
├── attendance.csv            # Daily time records
├── backups/                  # Auto-generated backup folder
└── README.md




