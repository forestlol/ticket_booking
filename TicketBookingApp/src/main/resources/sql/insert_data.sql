-- Insert Users
INSERT INTO User (email, password, name, type) VALUES
 ('customer1@gmail.com', 'password', 'Cust', 'Customer'),
 ('admin@gmail.com', 'password', 'Admin', 'EventManager'),
 ('officer1@gmail.com', 'password', 'T_Officer','TicketOfficer');

-- Insert Customer
INSERT INTO Customer (userID, accountBalance) VALUES (1, 1000.00);

-- Insert EventManager
INSERT INTO EventManager (userID) VALUES (2);

-- Insert TicketOfficer
INSERT INTO TicketOfficer (userID) VALUES (3);

-- Insert Events
-- INSERT INTO Event (eventID, managerID, basePrice, eventName, venue, startTime, duration, ticketCancellationFee, isCancelled) VALUES
-- ('E001', 'U001', 50.00, 'Concert', 'Concert Hall', '2023-12-25 19:00:00', 120, 5.00, 0);

-- Insert AuthorisedOfficers
-- INSERT INTO AuthorisedOfficers (ticketOfficerID, eventID, timeStamp) VALUES
-- ('U002', 'E001', '2023-12-01 00:00:00');

-- Insert TicketOptions
-- INSERT INTO TicketOption (ticketOptionID, eventID, name, priceMultiplier, totalAvailable) VALUES
-- ('TO001', 'E001', 'VIP', 2, 50),
-- ('TO002', 'E001', 'Standard', 1, 100);

-- Insert Tickets
-- INSERT INTO Ticket (ticketID, bookingID, isGuest, attended) VALUES
-- ('T001', 'B001', 0, 0);

-- Insert Bookings
-- INSERT INTO Booking (bookingID, eventID, ticketOptionID, customerID, ticketOfficerID, bookedTime) VALUES
-- ('B001', 'E001', 'TO001', 'U003', 'U002', '2023-12-24 10:00:00');


-- Insert Refunds
-- INSERT INTO Refund (bookingID, refundDate, refundStatus) VALUES

