import { useNavigate } from "react-router-dom";
import { getLoggedUser, removeLoggedUser } from "../../utils/authStorage.js";

function AdminDashboardPage() {
    const navigate = useNavigate();
    const user = getLoggedUser();
    const handleLogout = () => {
        removeLoggedUser();
        navigate("/login");
    };

    return (
        <div>
            <h1>Admin Dashboard</h1>

            <p>
                Welcome, <strong>{user.username}</strong>
            </p>

            <p>Email: {user.email}</p>

            <hr />

            <h2>Administrative Actions</h2>

            <ul>
                <li>Unlock blocked users (Coming soon)</li>
                <li>Configure global bank limits (Coming soon)</li>
                <li>Create shared accounts (Coming soon)</li>
                <li>Revoke shared account access (Coming soon)</li>
            </ul>

            <button onClick={handleLogout}>
                Logout
            </button>
        </div>
    );
}

export default AdminDashboardPage;