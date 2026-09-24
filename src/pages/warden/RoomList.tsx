import React, { useEffect, useState } from 'react';
import { Search, Eye } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { StatusBadge } from '../../components/common/StatusBadge';
import { EmptyState } from '../../components/common/EmptyState';
import { RoomDetailModal } from './RoomDetailModal';
import { apiService } from '../../services/api';
import { Room } from '../../types';

export const RoomList: React.FC = () => {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [blockFilter, setBlockFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedRoom, setSelectedRoom] = useState<Room | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    apiService.getRooms().then((res) => {
      setRooms(res);
      setIsLoading(false);
    });
  }, []);

  const filtered = rooms.filter((r) => {
    const matchesSearch =
      r.roomNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.occupants.some((occ) => occ.name.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesBlock = blockFilter === 'ALL' || r.block === blockFilter;
    const matchesStatus = statusFilter === 'ALL' || r.status === statusFilter;
    return matchesSearch && matchesBlock && matchesStatus;
  });

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-900">Hostel Rooms & Inventory</h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Monitor room occupancies, bed allocations, and resident room associations
        </p>
      </div>

      {/* Filter bar */}
      <Card className="!p-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
            <input
              type="text"
              placeholder="Search room number or resident name..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <select
            value={blockFilter}
            onChange={(e) => setBlockFilter(e.target.value)}
            className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="ALL">All Blocks</option>
            <option value="Block A">Block A</option>
            <option value="Block B">Block B</option>
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="ALL">All Occupancy States</option>
            <option value="Available">Available (Vacant)</option>
            <option value="Partially Occupied">Partially Occupied</option>
            <option value="Full">Full</option>
          </select>
        </div>
      </Card>

      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Loading room inventory...</div>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No rooms found"
          description="Try changing the block or occupancy filter options."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((room) => {
            const occupancyPct = Math.round((room.occupied / room.capacity) * 100);
            return (
              <Card
                key={room.id}
                className="hover:border-blue-300 transition-all hover:shadow-md cursor-pointer"
                onClick={() => setSelectedRoom(room)}
              >
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <h3 className="text-base font-bold text-slate-900">
                      Room {room.roomNumber}
                    </h3>
                    <p className="text-xs text-slate-500 font-medium">
                      {room.block} • Floor {room.floor}
                    </p>
                  </div>
                  <StatusBadge status={room.status} size="sm" />
                </div>

                {/* Occupancy Indicator */}
                <div className="space-y-1 mb-4">
                  <div className="flex justify-between text-xs">
                    <span className="text-slate-500">Beds Occupied</span>
                    <span className="font-semibold text-slate-800">
                      {room.occupied} / {room.capacity} ({occupancyPct}%)
                    </span>
                  </div>
                  <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                    <div
                      className={`h-2 rounded-full ${
                        room.occupied === room.capacity
                          ? 'bg-slate-400'
                          : room.occupied > 0
                          ? 'bg-blue-600'
                          : 'bg-emerald-500'
                      }`}
                      style={{ width: `${occupancyPct}%` }}
                    />
                  </div>
                </div>

                {/* Occupants Preview */}
                <div className="border-t border-slate-100 pt-3">
                  <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block mb-2">
                    Residents ({room.occupants.length})
                  </span>
                  {room.occupants.length === 0 ? (
                    <p className="text-xs text-emerald-600 font-medium italic">Empty room available</p>
                  ) : (
                    <div className="space-y-1.5">
                      {room.occupants.map((occ) => (
                        <div key={occ.studentId} className="flex justify-between text-xs">
                          <span className="font-medium text-slate-700 truncate mr-2">{occ.name}</span>
                          <span className="text-slate-400 font-mono text-[11px] flex-shrink-0">
                            {occ.studentId}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-blue-600 font-medium">
                  <span>View Details</span>
                  <Eye className="w-4 h-4" />
                </div>
              </Card>
            );
          })}
        </div>
      )}

      <RoomDetailModal
        isOpen={!!selectedRoom}
        onClose={() => setSelectedRoom(null)}
        room={selectedRoom}
      />
    </div>
  );
};
