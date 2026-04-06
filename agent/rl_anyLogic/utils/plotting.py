import matplotlib.pyplot as plt


def plot_bullwhip(history):
    """
    history = {
        "retailer": [...],
        "distributor": [...],
        "manufacturer": [...]
    }
    """

    plt.plot(history["retailer"], label="Retailer")
    plt.plot(history["distributor"], label="Distributor")
    plt.plot(history["manufacturer"], label="Manufacturer")

    plt.title("Bullwhip Effect Over Training")
    plt.legend()
    plt.show()