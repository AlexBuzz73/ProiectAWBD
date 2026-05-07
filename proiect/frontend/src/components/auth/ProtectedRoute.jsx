import { Navigate } from "react-router-dom";
import { getLoggedUser } from "../../utils/authStorage.js";

function ProtectedRoute({ requiredRole, children }) {
    const user = getLoggedUser();

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (requiredRole && user.role !== requiredRole) {
        return <Navigate to="/login" replace />;
    }

    return children;
}

export default ProtectedRoute;