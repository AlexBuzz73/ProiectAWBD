import { Link } from "react-router-dom";

function AccountCard({ account }) {
    const statusClass = `status-${account.status?.toLowerCase() || "active"}`;

    return (
        <div className={`account-card card-status ${statusClass}`}>
            <h3>{account.alias}</h3>
            <div className="balance">{account.balance?.toLocaleString("ro-RO", { minimumFractionDigits: 2 })} {account.currency}</div>
            <div className="iban">{account.iban}</div>
            <p><strong>Rol:</strong> {account.accountRole}</p>
            <Link to={`/accounts/${account.accountId}`}>Vezi detalii →</Link>
        </div>
    );
}

export default AccountCard;
