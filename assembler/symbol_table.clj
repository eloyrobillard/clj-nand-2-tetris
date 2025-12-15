(ns assembler.symbol-table)

(defn add-entry [st key val]
  {:pre [(map? st) (string? key) (int? val)]}
  (assoc st key val))

(defn contains [st key]
  (some? (st key)))

(defn get-address [st key]
  {:post [(int? %)]}
  (st key))
