import java.io.*;
import java.util.Base64;

public class Order implements Serializable {
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

    public String serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        oos.flush();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public static Order deserialize(String str) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(str);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        return (Order) ois.readObject();
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public double getValuePerAsset() {
        return valuePerAsset;
    }

    public void setValuePerAsset(double valuePerAsset) {
        this.valuePerAsset = valuePerAsset;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getAssetID() {
        return assetID;
    }

    public void setAssetID(String assetID) {
        this.assetID = assetID;
    }
}