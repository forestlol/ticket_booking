package service;

import data.Refund;

import java.util.List;

public class RefundService {
    private DatabaseService databaseService;
    public RefundService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public List<Refund> getRefundsByUser()
    {
        return this.databaseService.getRefundsByUser(AccountService.getCurrentUser().getID());
    }

    public List<Refund> getRefundsByEvent(int eventID)
    {
        return this.databaseService.getRefundsByEvent(eventID);
    }

}
