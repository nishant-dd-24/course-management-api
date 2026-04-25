import { useAuth } from "./auth/AuthContext";
import Login from "./pages/Login";
import Users from "./pages/Users";

function FullScreenMessage({ text }) {
    return (
        <div className="h-screen flex items-center justify-center bg-gray-900 text-gray-100">
            {text}
        </div>
    );
}

export default function App() {
    const { status, isAuthenticated } = useAuth();

    if (status === "loading") {
        return <FullScreenMessage text="Restoring session..." />;
    }

    if (!isAuthenticated) {
        return <Login />;
    }

    return <Users />;
}

