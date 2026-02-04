'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { getUser } from '@/lib/api';

interface AdminGuardProps {
    children: React.ReactNode;
}

export default function AdminGuard({ children }: AdminGuardProps) {
    const [loading, setLoading] = useState(true);
    const [authorized, setAuthorized] = useState(false);
    const router = useRouter();

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, async (user) => {
            if (!user) {
                router.push('/login');
            } else {
                const profile = await getUser(user.uid);
                if (profile?.role === 'admin') {
                    setAuthorized(true);
                    setLoading(false);
                } else {
                    router.push('/');
                }
            }
        });

        return () => unsubscribe();
    }, [router]);

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="flex flex-col items-center gap-4">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
                    <div className="text-gray-600 font-medium">Verificando acceso...</div>
                </div>
            </div>
        );
    }

    return authorized ? <>{children}</> : null;
}
