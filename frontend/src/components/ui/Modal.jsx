import { useEffect } from "react";
import { X } from "lucide-react";
import { cn } from "../../lib/cn";

export default function Modal({ isOpen, onClose, title, description, children, className }) {
    useEffect(() => {
        if (isOpen) {
            document.body.style.overflow = "hidden";
        } else {
            document.body.style.overflow = "";
        }
        return () => {
            document.body.style.overflow = "";
        };
    }, [isOpen]);

    if (!isOpen) {
        return null;
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center animate-in">
            {/* Backdrop */}
            <div 
                className="fixed inset-0 bg-zinc-950/80 backdrop-blur-sm transition-opacity" 
                onClick={onClose}
                aria-hidden="true"
            />
            
            {/* Modal Dialog */}
            <div 
                role="dialog"
                aria-modal="true"
                className={cn(
                    "relative z-50 w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-2xl border border-zinc-800/80 bg-zinc-900/95 p-6 shadow-2xl animate-slide-up mx-4",
                    className
                )}
            >
                <div className="flex flex-col space-y-1.5 text-center sm:text-left mb-5">
                    <div className="flex items-start justify-between">
                        <h2 className="text-lg font-semibold tracking-tight text-zinc-50">{title}</h2>
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-full p-1.5 text-zinc-400 transition-colors hover:bg-zinc-800 hover:text-zinc-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-500"
                        >
                            <X className="h-4 w-4" />
                        </button>
                    </div>
                    {description && (
                        <p className="text-sm text-zinc-400">
                            {description}
                        </p>
                    )}
                </div>
                
                <div className="relative">
                    {children}
                </div>
            </div>
        </div>
    );
}
