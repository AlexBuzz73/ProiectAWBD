import { Link, useNavigate } from "react-router-dom";
import { getLoggedUser, removeLoggedUser } from "../../utils/authStorage.js";
import { logoutUser } from "../../api/authApi.js";

function Navbar() {
    const navigate = useNavigate();
    const user = getLoggedUser();

    const handleLogout = async () => {
        try{
            await logoutUser();
        } finally {
            removeLoggedUser();
            navigate("/login", { replace:true });
        }
    };

    if (!user) return null;

    const isAdmin = user.role === "ADMIN";

    return (
        <nav className="navbar">
            <Link to={isAdmin ? "/admin/dashboard" : "/dashboard"} className="navbar-brand">
                Internet Banking
            </Link>

            {!isAdmin && (
                <ul className="navbar-links">
                    <li><Link to="/dashboard">Dashboard</Link></li>
                    <li><Link to="/accounts/new">Conturi</Link></li>
                    <li><Link to="/payments/new">Plată</Link></li>
                    <li><Link to="/transfer">Transfer</Link></li>
                    <li><Link to="/exchange">Schimb valutar</Link></li>
                    <li><Link to="/categories">Categorii</Link></li>
                    <li><Link to="/user-limits">Limite</Link></li>
                </ul>
            )}

            {isAdmin && (
                <ul className="navbar-links">
                    <li><Link to="/admin/dashboard">Dashboard</Link></li>
                    <li><Link to="/admin/unlock-user">Deblocare</Link></li>
                    <li><Link to="/admin/shared-account">Cont partajat</Link></li>
                    <li><Link to="/admin/bank-limits">Limite globale</Link></li>
                    <li><Link to="/admin/revoke-access">Revocare acces</Link></li>
                </ul>
            )}

            <div className="navbar-user">
                <span>👤 {user.username}</span>
                <button
                    type="button"
                    onClick={handleLogout}
                    style={{
                        background: "rgba(255,255,255,0.12)",
                        color: "#fff",
                        border: "1px solid rgba(255,255,255,0.2)",
                        padding: "6px 14px",
                        fontSize: "0.8125rem",
                        cursor: "pointer",
                        borderRadius: "6px",
                        fontWeight: 600,
                    }}
                >
                    Logout
                </button>
            </div>
        </nav>
    );
}

export default Navbar;
