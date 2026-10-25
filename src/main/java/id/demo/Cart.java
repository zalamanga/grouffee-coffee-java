package id.demo;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Cart implements Serializable {
  private final Map<Long, Integer> items = new LinkedHashMap<>();

  public synchronized void add(long id, int quantity) {
    if (id < 1 || quantity < 1 || quantity > 99)
      throw new IllegalArgumentException("Jumlah harus 1 sampai 99.");
    if (!items.containsKey(id) && items.size() >= 20)
      throw new IllegalArgumentException("Keranjang dibatasi 20 jenis menu.");
    int total = items.getOrDefault(id, 0) + quantity;
    if (total > 99) throw new IllegalArgumentException("Maksimum 99 item untuk setiap menu.");
    items.put(id, total);
  }

  public synchronized void remove(long id) {
    items.remove(id);
  }

  public synchronized Map<Long, Integer> snapshot() {
    return new LinkedHashMap<>(items);
  }

  public synchronized void clear() {
    items.clear();
  }
}
