import { useEffect, useState } from "react";
import { createCard, getCardForAccount, updateCardStatus } from "../../api/cardsApi.js";

function maskCardNumber(cardNumber) {
    if (!cardNumber) {
        return "";
    }

    return `**** **** **** ${cardNumber.slice(-4)}`;
}

function formatDate(dateValue) {
    if (!dateValue) {
        return "-";
    }

    return new Date(dateValue).toLocaleDateString();
}

function AccountCardSection({ userId, account }) {
    const [card, setCard] = useState(null);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const isOwner = account?.accountRole === "OWNER";

    useEffect(() => {
        loadCard();
    }, [userId, account?.accountId]);

    const loadCard = async () => {
        if (!userId || !account?.accountId) {
            return;
        }

        setLoading(true);
        setError("");
        setMessage("");

        try {
            const response = await getCardForAccount(userId, account.accountId);
            setCard(response);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleCreateCard = async () => {
        const confirmed = window.confirm("Are you sure you want to order a card for this account?");

        if (!confirmed) {
            return;
        }

        setActionLoading(true);
        setError("");
        setMessage("");

        try {
            const response = await createCard(userId, account.accountId);

            setCard(response);
            setMessage("Card created successfully.");
        } catch (err) {
            setError(err.message);
        } finally {
            setActionLoading(false);
        }
    };

    const handleUpdateStatus = async (newStatus) => {
        setActionLoading(true);
        setError("");
        setMessage("");

        try {
            await updateCardStatus(userId, account.accountId, card.cardId, newStatus);

            setCard({
                ...card,
                status: newStatus,
            });

            setMessage(newStatus === "BLOCKED"
                ? "Card blocked successfully."
                : "Card unblocked successfully."
            );
        } catch (err) {
            setError(err.message);
        } finally {
            setActionLoading(false);
        }
    };

    return (
        <div>
            <h2>Card</h2>

            {loading && <p>Loading card...</p>}

            {error && (
                <p style={{ color: "red" }}>
                    {error}
                </p>
            )}

            {message && (
                <p style={{ color: "green" }}>
                    {message}
                </p>
            )}

            {!loading && !card && (
                <div>
                    <p>No card is currently associated with this account.</p>

                    {isOwner ? (
                        <button
                            type="button"
                            onClick={handleCreateCard}
                            disabled={actionLoading}
                        >
                            {actionLoading ? "Ordering card..." : "Order Card"}
                        </button>
                    ) : (
                        <p>
                            Only the account owner can order a card.
                        </p>
                    )}
                </div>
            )}

            {!loading && card && (
                <div>
                    <div
                        style={{
                            border: "1px solid #ccc",
                            borderRadius: "12px",
                            padding: "16px",
                            maxWidth: "360px",
                            marginBottom: "16px",
                        }}
                    >
                        <h3>{card.type} Card</h3>

                        <p>
                            <strong>Card Number:</strong> {maskCardNumber(card.cardNumber)}
                        </p>

                        <p>
                            <strong>Holder:</strong> {card.holderName}
                        </p>

                        <p>
                            <strong>Expiration Date:</strong> {formatDate(card.expirationDate)}
                        </p>

                        <p>
                            <strong>Status:</strong> {card.status}
                        </p>
                    </div>

                    {isOwner && card.status === "ACTIVE" && (
                        <button
                            type="button"
                            onClick={() => handleUpdateStatus("BLOCKED")}
                            disabled={actionLoading}
                        >
                            {actionLoading ? "Blocking card..." : "Block Card"}
                        </button>
                    )}

                    {isOwner && card.status === "BLOCKED" && (
                        <button
                            type="button"
                            onClick={() => handleUpdateStatus("ACTIVE")}
                            disabled={actionLoading}
                        >
                            {actionLoading ? "Unblocking card..." : "Unblock Card"}
                        </button>
                    )}
                </div>
            )}
        </div>
    );
}

export default AccountCardSection;