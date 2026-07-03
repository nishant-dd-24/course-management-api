import Card from "../ui/Card";
import PageHeader from "../ui/PageHeader";

function ActionCard({ title, description, onClick }) {
    return (
        <Card
            as="button"
            type="button"
            onClick={onClick}
            className="group w-full text-left transition-transform duration-150 hover:-translate-y-0.5 hover:border-slate-700"
        >
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h3 className="text-base font-semibold text-slate-50">{title}</h3>
                    <p className="mt-2 text-sm leading-6 text-slate-300">{description}</p>
                </div>
                <div className="mt-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-slate-800 text-slate-400 transition-colors duration-150 group-hover:bg-slate-700 group-hover:text-slate-200">
                    →
                </div>
            </div>
        </Card>
    );
}

function actionsByRole(role) {
    if (role === "ADMIN") {
        return [
            {
                title: "Manage Users",
                description: "Review and manage all users in the system.",
                path: "/users",
            },
            {
                title: "View Courses",
                description: "Inspect available courses and platform activity.",
                path: "/courses",
            },
        ];
    }

    if (role === "INSTRUCTOR") {
        return [
            {
                title: "My Courses",
                description: "Open your courses and manage course details.",
                path: "/courses",
            },
            {
                title: "Create Course",
                description: "Go to course area and create a new course.",
                path: "/courses",
            },
            {
                title: "Course Enrollments",
                description: "Track enrollments and activity for your courses.",
                path: "/enrollments",
            },
        ];
    }

    return [
        {
            title: "Available Courses",
            description: "Browse open courses and enrollment options.",
            path: "/courses",
        },
        {
            title: "My Enrollments",
            description: "View your active and historical enrollments.",
            path: "/enrollments",
        },
    ];
}

export default function DashboardContent({ role, onNavigate }) {
    const actions = actionsByRole(role);

    return (
        <main className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8">
            <PageHeader
                eyebrow="Overview"
                title="Dashboard"
                subtitle="Use the quick actions below to jump into your most common tasks."
            />

            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                {actions.map((action) => (
                    <ActionCard
                        key={`${action.path}-${action.title}`}
                        title={action.title}
                        description={action.description}
                        onClick={() => onNavigate(action.path)}
                    />
                ))}
            </section>
        </main>
    );
}

