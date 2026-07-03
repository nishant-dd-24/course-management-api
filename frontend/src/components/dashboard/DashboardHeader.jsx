import Navigation from "../ui/Navigation";

export default function DashboardHeader({ user, onLogout, onNavigate }) {
    return <Navigation user={user} onLogout={onLogout} onNavigate={onNavigate} />;
}

