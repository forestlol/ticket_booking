-- User Supertype
CREATE TABLE IF NOT EXISTS User (
    userID INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(30) NOT NULL,
    password VARCHAR(30) NOT NULL,
    name VARCHAR(30) NOT NULL,
    type VARCHAR(30) NOT NULL
);


-- Customer Subtype
CREATE TABLE IF NOT EXISTS Customer (
    userID INT PRIMARY KEY,
    accountBalance DOUBLE NOT NULL,
    FOREIGN KEY (userID) REFERENCES User(userID)
);

-- EventManager Subtype
CREATE TABLE IF NOT EXISTS EventManager (
    userID INT PRIMARY KEY,
    FOREIGN KEY (userID) REFERENCES User(userID)
);

-- TicketOfficer Subtype
CREATE TABLE IF NOT EXISTS TicketingOfficer (
    userID INT PRIMARY KEY,
    FOREIGN KEY (userID) REFERENCES User(userID)
);

-- Event Table
CREATE TABLE IF NOT EXISTS Event (
    eventID INT PRIMARY KEY AUTO_INCREMENT,
    eventManagerID INT NOT NULL,
    basePrice DOUBLE NOT NULL,
    eventName VARCHAR(30) NOT NULL,
    eventDesc VARCHAR (200) NOT NULL,
    venue VARCHAR(50) NOT NULL,
    startTime DATETIME NOT NULL,
    duration INT NOT NULL,
    revenue DOUBLE NOT NULL,
    currSlots INT NOT NULL,
    totalSlots INT NOT NULL,
    ticketCancellationFee DOUBLE NOT NULL,
    isCancelled TINYINT NOT NULL,
    FOREIGN KEY (eventmanagerID) REFERENCES EventManager(userID)
);

-- AuthorisedOfficers Associative Table
CREATE TABLE IF NOT EXISTS AuthorisedOfficers (
    ticketingOfficerID INT NOT NULL,
    eventID INT NOT NULL,
    timeStamp DATETIME NOT NULL,
    PRIMARY KEY (ticketingOfficerID, eventID),
    FOREIGN KEY (ticketingOfficerID) REFERENCES TicketingOfficer(userID),
    FOREIGN KEY (eventID) REFERENCES Event(eventID)
);

-- TicketOption Table
CREATE TABLE IF NOT EXISTS TicketOption (
    ticketOptionID INT PRIMARY KEY AUTO_INCREMENT,
    eventID INT NOT NULL,
    optionName VARCHAR(30) NOT NULL,
    priceMultiplier DOUBLE NOT NULL,
    totalAvailable INT NOT NULL,
    FOREIGN KEY (eventID) REFERENCES Event(eventID)
);

-- Ticket Table
CREATE TABLE IF NOT EXISTS Ticket (
    ticketID INT PRIMARY KEY AUTO_INCREMENT,
    bookingID INT NOT NULL,
    isGuest TINYINT NOT NULL,
    attended TINYINT NOT NULL,
    FOREIGN KEY (bookingID) REFERENCES Booking(bookingID)
);

-- Booking Table
CREATE TABLE IF NOT EXISTS Booking (
    bookingID INT PRIMARY KEY AUTO_INCREMENT,
    eventID INT NOT NULL,
    ticketOptionID INT NOT NULL,
    customerID INT NOT NULL,
    ticketingOfficerID INT NOT NULL,
    numOfTickets INT NOT NULL,
    amountPaid DOUBLE NOT NULL,
    bookedTime DATETIME NOT NULL,
    bookingStatus VARCHAR(30) NOT NULL,
    FOREIGN KEY (eventID) REFERENCES Event(eventID),
    FOREIGN KEY (ticketOptionID) REFERENCES TicketOption(ticketOptionID),
    FOREIGN KEY (customerID) REFERENCES Customer(userID),
    FOREIGN KEY (ticketingOfficerID) REFERENCES TicketingOfficer(userID)
);

-- Refund Table
CREATE TABLE IF NOT EXISTS Refund (
    refundID INT PRIMARY KEY AUTO_INCREMENT,
    bookingID INT NOT NULL,
    refundDate DATETIME NOT NULL,
    refundStatus VARCHAR(30) NOT NULL,
    FOREIGN KEY (bookingID) REFERENCES Booking(bookingID)
);