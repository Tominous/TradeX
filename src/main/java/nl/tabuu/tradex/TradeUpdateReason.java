package nl.tabuu.tradex;

public enum TradeUpdateReason {
    OFFER_CHANGED,
    OFFER_CONFIRMED,

    TIMER_UPDATE,

    TRADE_CONFIRM_STOP,
    TRADE_CONFIRMED,
    TRADE_CANCELED,
    TRADE_COMPLETED
}
