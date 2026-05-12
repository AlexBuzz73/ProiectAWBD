import { Link } from "react-router-dom";

function AccountCard({ account }) {
    return (
        <div>
            <h3>{account.alias}</h3>

            <p>
                <strong>IBAN:</strong> {account.iban}
            </p>

            <p>
                <strong>Currency:</strong> {account.currency}
            </p>

            <p>
                <strong>Balance:</strong> {account.balance}
            </p>

            <p>
                <strong>Role:</strong> {account.accountRole}
            </p>

            <Link to={`/accounts/${account.accountId}`}>
                View Details
            </Link>
        </div>
    );
}

export default AccountCard;