public class Order {
    private String orderType;
    private double valuePerAsset;
    private int quantity;
    private String assetID;

    public Order(String orderType, double valuePerAsset, int quantity, String assetID) {
        this.orderType = orderType;
        this.valuePerAsset = valuePerAsset;
        this.quantity = quantity;
        this.assetID = assetID;
    }
}