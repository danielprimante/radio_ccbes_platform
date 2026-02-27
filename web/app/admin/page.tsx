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
    const [selectedPosts, setSelectedPosts] = useState<Set<string>>(new Set());
    const [filterTime, setFilterTime] = useState('all');
    const [sortBy, setSortBy] = useState('newest');
    const [searchQuery, setSearchQuery] = useState('');

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

    const toggleSelectAll = () => {
        if (selectedPosts.size === posts.length) {
            setSelectedPosts(new Set());
        } else {
            setSelectedPosts(new Set(posts.map(p => p.id!)));
        }
    };

    const toggleSelect = (id: string) => {
        const newSelected = new Set(selectedPosts);
        if (newSelected.has(id)) {
            newSelected.delete(id);
        } else {
            newSelected.add(id);
        }
        setSelectedPosts(newSelected);
    };

    const handleBatchDelete = async () => {
        if (!confirm(`¿Estás seguro de eliminar ${selectedPosts.size} publicaciones?`)) return;

        try {
            await Promise.all(Array.from(selectedPosts).map(id => deletePost(id)));
            setSelectedPosts(new Set());
            loadData();
        } catch (error) {
            console.error('Error deleting posts:', error);
            alert('Error al eliminar publicaciones');
        }
    };

    const filteredPosts = posts.filter(post => {
        // Search Filter
        const searchLower = searchQuery.toLowerCase();
        const matchesSearch = post.content.toLowerCase().includes(searchLower) ||
            post.userName.toLowerCase().includes(searchLower);

        // Time Filter
        let matchesTime = true;
        if (filterTime !== 'all') {
            const date = post.timestamp.toDate();
            const now = new Date();
            const diffTime = Math.abs(now.getTime() - date.getTime());
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

            if (filterTime === '1m') matchesTime = diffDays <= 30;
            else if (filterTime === '3m') matchesTime = diffDays <= 90;
            else if (filterTime === '6m') matchesTime = diffDays <= 180;
            else if (filterTime === '1y') matchesTime = diffDays <= 365;
        }

        return matchesSearch && matchesTime;
    }).sort((a, b) => {
        if (sortBy === 'newest') return b.timestamp.seconds - a.timestamp.seconds;
        if (sortBy === 'oldest') return a.timestamp.seconds - b.timestamp.seconds;
        if (sortBy === 'most_likes') return b.likes - a.likes;
        if (sortBy === 'least_likes') return a.likes - b.likes;
        if (sortBy === 'most_comments') return b.comments - a.comments;
        if (sortBy === 'least_comments') return a.comments - b.comments;
        return 0;
    });

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* Stats */}
            {stats && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8 max-w-5xl mx-auto">
                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow text-center">
                        <div className="text-sm font-medium text-gray-600">Total Publicaciones</div>
                        <div className="text-3xl font-bold text-gray-900 mt-2">{stats.totalPosts}</div>
                    </div>
                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow text-center">
                        <div className="text-sm font-medium text-gray-600">Total Likes</div>
                        <div className="text-3xl font-bold text-red-600 mt-2">{stats.totalLikes}</div>
                    </div>
                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 hover:shadow-md transition-shadow text-center">
                        <div className="text-sm font-medium text-gray-600">Total Comentarios</div>
                        <div className="text-3xl font-bold text-blue-600 mt-2">{stats.totalComments}</div>
                    </div>
                </div>
            )}

            {/* Filters Bar */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 mb-6 flex flex-col md:flex-row gap-4 justify-between items-center">
                <div className="relative w-full md:w-96">
                    <span className="material-symbols-outlined absolute left-3 top-2.5 text-gray-400">search</span>
                    <input
                        type="text"
                        placeholder="Buscar por contenido o autor..."
                        className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
                <div className="flex gap-4 w-full md:w-auto">
                    <select
                        className="px-4 py-2 rounded-lg border border-gray-200 bg-white focus:outline-none focus:ring-2 focus:ring-red-500"
                        value={filterTime}
                        onChange={(e) => setFilterTime(e.target.value)}
                    >
                        <option value="all">Todo el tiempo</option>
                        <option value="1m">Último mes</option>
                        <option value="3m">Últimos 3 meses</option>
                        <option value="6m">Últimos 6 meses</option>
                        <option value="1y">Último año</option>
                    </select>
                    <select
                        className="px-4 py-2 rounded-lg border border-gray-200 bg-white focus:outline-none focus:ring-2 focus:ring-red-500"
                        value={sortBy}
                        onChange={(e) => setSortBy(e.target.value)}
                    >
                        <option value="newest">Más recientes</option>
                        <option value="oldest">Más antiguos</option>
                        <option value="most_likes">Más likes</option>
                        <option value="least_likes">Menos likes</option>
                        <option value="most_comments">Más comentarios</option>
                        <option value="least_comments">Menos comentarios</option>
                    </select>
                </div>
            </div>

            {/* Posts Table */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="px-6 py-5 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
                    <div className="flex items-center gap-4">
                        <h2 className="text-lg font-bold text-gray-900">Publicaciones Recientes</h2>
                        {selectedPosts.size > 0 && (
                            <button
                                onClick={handleBatchDelete}
                                className="bg-red-100 text-red-700 px-3 py-1 rounded-lg text-sm font-semibold hover:bg-red-200 transition-colors flex items-center gap-2"
                            >
                                <span className="material-symbols-outlined text-sm">delete</span>
                                Eliminar ({selectedPosts.size})
                            </button>
                        )}
                    </div>
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
                                <th className="px-6 py-4 text-left">
                                    <input
                                        type="checkbox"
                                        checked={selectedPosts.size === posts.length && posts.length > 0}
                                        onChange={toggleSelectAll}
                                        className="rounded border-gray-300 text-red-600 focus:ring-red-500"
                                    />
                                </th>
                                <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Contenido</th>
                                <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Categoría</th>
                                <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Estadísticas</th>
                                <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Acciones</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-100">
                            {filteredPosts.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="px-6 py-12 text-center text-gray-500">
                                        No se encontraron publicaciones con estos filtros.
                                    </td>
                                </tr>
                            ) : (
                                filteredPosts.map((post) => (
                                    <tr key={post.id} className={`hover:bg-gray-50/50 transition-colors ${selectedPosts.has(post.id!) ? 'bg-red-50/30' : ''}`}>
                                        <td className="px-6 py-4">
                                            <input
                                                type="checkbox"
                                                checked={selectedPosts.has(post.id!)}
                                                onChange={() => toggleSelect(post.id!)}
                                                className="rounded border-gray-300 text-red-600 focus:ring-red-500"
                                            />
                                        </td>
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
                                )))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
