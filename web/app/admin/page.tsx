'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { getPosts, getStats, deletePost, getUser, type Post } from '@/lib/api';
import Link from 'next/link';

import AdminGuard from '@/components/AdminGuard';

export default function AdminDashboard() {
    const [posts, setPosts] = useState<Post[]>([]);
    const [stats, setStats] = useState<any>(null);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            const [postsData, statsData] = await Promise.all([
                getPosts(),
                getStats()
            ]);
            setPosts(postsData);
            setStats(statsData);
        } catch (error) {
            console.error('Error loading data:', error);
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm('¿Estás seguro de eliminar esta publicación?')) return;

        try {
            await deletePost(id);
            loadData();
        } catch (error) {
            console.error('Error deleting post:', error);
            alert('Error al eliminar la publicación');
        }
    };

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* Stats */}
            {stats && (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                        <div className="text-sm font-medium text-gray-600">Total Publicaciones</div>
                        <div className="text-3xl font-bold text-gray-900 mt-2">{stats.totalPosts}</div>
                    </div>
                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                        <div className="text-sm font-medium text-gray-600">Total Likes</div>
                        <div className="text-3xl font-bold text-red-600 mt-2">{stats.totalLikes}</div>
                    </div>
                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                        <div className="text-sm font-medium text-gray-600">Total Comentarios</div>
                        <div className="text-3xl font-bold text-blue-600 mt-2">{stats.totalComments}</div>
                    </div>
                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow">
                        <div className="text-sm font-medium text-gray-600">Categorías</div>
                        <div className="mt-2 space-y-1">
                            <div className="text-xs text-gray-600">Tendencias: {stats.categoryCount.trending}</div>
                            <div className="text-xs text-gray-600">Noticias: {stats.categoryCount.news}</div>
                            <div className="text-xs text-gray-600">Reflexiones: {stats.categoryCount.reflections}</div>
                        </div>
                    </div>
                </div>
            )}

            {/* Analytics Chart Placeholder */}
            {/* ... (keep as is but optimized styles if possible) */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-8">
                <h2 className="text-lg font-bold text-gray-900 mb-4">Interacción Diaria</h2>
                <div className="h-64 bg-gray-50 flex items-center justify-center rounded-xl border-2 border-dashed border-gray-200">
                    <div className="text-center">
                        <p className="text-gray-500 font-medium">Métricas en Tiempo Real</p>
                        <p className="text-xs text-gray-400 mt-1">Sincronizando con Firestore...</p>
                        <div className="flex gap-2 justify-center mt-6 h-32 items-end">
                            <div className="w-6 h-[40%] bg-red-100 rounded-t-lg transition-all hover:bg-red-500"></div>
                            <div className="w-6 h-[70%] bg-red-200 rounded-t-lg transition-all hover:bg-red-500"></div>
                            <div className="w-6 h-[30%] bg-red-100 rounded-t-lg transition-all hover:bg-red-500"></div>
                            <div className="w-6 h-[90%] bg-red-600 rounded-t-lg transition-all hover:bg-red-500"></div>
                            <div className="w-6 h-[60%] bg-red-300 rounded-t-lg transition-all hover:bg-red-500"></div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Posts Table */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="px-6 py-5 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
                    <h2 className="text-lg font-bold text-gray-900">Publicaciones Recientes</h2>
                    <Link
                        href="/admin/posts/new"
                        className="bg-red-600 text-white px-4 py-2 rounded-xl font-semibold hover:bg-red-700 transition-all shadow-sm hover:shadow-md text-sm"
                    >
                        + Crear Post
                    </Link>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-50/50">
                            <tr>
                                <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Contenido</th>
                                <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Categoría</th>
                                <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Estadísticas</th>
                                <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Acciones</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-100">
                            {posts.map((post) => (
                                <tr key={post.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="px-6 py-4">
                                        <div className="text-sm font-medium text-gray-900 line-clamp-1">{post.content}</div>
                                        <div className="text-xs text-gray-400 mt-0.5">Por {post.userName}</div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <span className="px-2.5 py-1 text-xs font-bold rounded-lg bg-red-50 text-red-600 uppercase">
                                            {post.category}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="flex gap-4 text-xs font-medium text-gray-500">
                                            <span className="flex items-center gap-1">❤️ {post.likes}</span>
                                            <span className="flex items-center gap-1">💬 {post.comments}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                        <div className="flex gap-3">
                                            <Link href={`/admin/posts/edit?id=${post.id}`} className="text-blue-600 hover:text-blue-800 transition-colors">Editar</Link>
                                            <button onClick={() => handleDelete(post.id!)} className="text-red-400 hover:text-red-600 transition-colors">Eliminar</button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
