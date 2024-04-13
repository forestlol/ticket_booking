-- Insert Users
INSERT INTO User (email, password, name, type) VALUES
 ('customer@gmail.com', 'password', 'Cust', 'Customer'),
 ('admin@gmail.com', 'password', 'Admin', 'EventManager'),
 ('officer@gmail.com', 'password', 'T_Officer','TicketingOfficer');

-- Insert Customer
INSERT INTO Customer (userID, accountBalance) VALUES (1, 1000.00);

-- Insert EventManager
INSERT INTO EventManager (userID) VALUES (2);

-- Insert TicketingOfficer
INSERT INTO TicketingOfficer (userID) VALUES (3);

-- Insert Events
INSERT INTO Event (eventManagerID, basePrice, eventName, eventDesc, venue, startTime, duration, revenue, currSlots, totalSlots, ticketCancellationFee, isCancelled) VALUES
    (2, 50.00, 'Tech Conference 2024', 'An annual conference on the latest tech trends.', 'Convention Center', '2024-07-15 09:00:00', 480, 0.00, 200, 200, 10.00, 0),
    (2, 75.00, 'Live Music Festival', 'A weekend of live music from top artists around the world.', 'Open Air Park', '2024-08-20 15:00:00', 240, 0.00, 500, 500, 15.00, 0),
    (2, 40.00, 'Art & Wine Fair', 'Explore fine arts and taste exquisite wines.', 'Downtown Gallery', '2024-09-10 11:00:00', 120, 0.00, 150, 150, 5.00, 0),
    (2, 30.00, 'November Concert', 'For the purpose of more than 6 months.', 'Kallang Stadium', '2024-11-10 11:00:00', 180, 0.00, 200, 200, 45.00,0),
    (2, 30.00, 'April Concert', 'For the purpose of less than 24 hours.', 'Kallang Stadium', '2024-04-14 18:00:00', 75, 0.00, 200, 200, 45.00,0);


-- Insert Ticket Options for Tech Conference 2024
INSERT INTO TicketOption (eventID, optionName, priceMultiplier, totalAvailable) VALUES
    (1, 'CAT1', 2.00, 50),
    (1, 'CAT2', 1.50, 50),
    (1, 'CAT3', 1.00, 100);

-- Insert Ticket Options for Live Music Festival
INSERT INTO TicketOption (eventID, optionName, priceMultiplier, totalAvailable) VALUES
    (2, 'CAT1', 2.00, 50),
    (2, 'CAT2', 1.75, 50),
    (2, 'CAT3', 1.50, 100),
    (2, 'CAT4', 1.25, 150),
    (2, 'CAT5', 1.00, 150);

-- Insert Ticket Options for Art & Wine Fair
INSERT INTO TicketOption (eventID, optionName, priceMultiplier, totalAvailable) VALUES
    (3, 'Standard', 1.00, 100),
    (3, 'VIP', 1.50, 50);

-- Insert Ticket Options for November Concert
INSERT INTO TicketOption (eventID, optionName, priceMultiplier, totalAvailable) VALUES
    (4, 'CAT1', 2.00, 50),
    (4, 'CAT2', 1.50, 50),
    (4, 'CAT3', 1.00, 100);

-- Insert Ticket Options for April Concert
INSERT INTO TicketOption (eventID, optionName, priceMultiplier, totalAvailable) VALUES
    (5, 'Standard', 2.00, 50),
    (5, 'VIP', 1.50, 25);