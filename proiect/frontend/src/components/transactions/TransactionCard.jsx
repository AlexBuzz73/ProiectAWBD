function TransactionCard({ transaction }) {
    const formattedDate = new Date(transaction.createdAt).toLocaleString();

    return (
        <div>
            <p>
                <strong>Type:</strong> {transaction.transactionType}
            </p>

            <p>
                <strong>Amount:</strong> {transaction.amount} {transaction.currency}
            </p>

            <p>
                <strong>Status:</strong> {transaction.status}
            </p>

            <p>
                <strong>Description:</strong>{" "}
                {transaction.description || "-"}
            </p>

            <p>
                <strong>Category:</strong>{" "}
                {transaction.categoryName || "-"}
            </p>

            <p>
                <strong>Date:</strong> {formattedDate}
            </p>

            <p>
                <strong>Source:</strong>{" "}
                {transaction.sourceAccountAlias || transaction.sourceAccountIban || "-"}
            </p>

            <p>
                <strong>Destination account:</strong>{" "}
                {
                    transaction.destinationAccountAlias
                    || transaction.destinationAccountIban
                    || transaction.destinationIban
                    || "-"
                }
            </p>

            <hr />
        </div>
    );
}

export default TransactionCard;