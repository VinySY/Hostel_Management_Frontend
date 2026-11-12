import React, { useState, useEffect } from 'react';
import {
  ClipboardList,
  Plus,
  BarChart2,
  TrendingUp,
  MessageSquare,
  ArrowRight,
  Send,
  Power,
  Users,
  CheckCircle2,
  AlertTriangle,
  Award,
  Layers,
  Calendar
} from 'lucide-react';
import { apiService } from '../../services/api';
import { Survey, SurveyAnalytics, Hostel, Warden } from '../../types';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { EmptyState } from '../../components/common/EmptyState';

const QUESTION_LABELS: Record<string, string> = {
  q1Cleanliness: 'Cleanliness & Common Areas',
  q2Washrooms: 'Washroom Sanitization',
  q3Furniture: 'Room Furniture & Beds',
  q4DrinkingWater: 'RO Drinking Water',
  q5Wifi: 'Wi-Fi & Connectivity',
  q6Electricity: 'Electricity & Power Backup',
  q7Food: 'Mess Food Quality',
  q8Security: 'Security & Guard Vigilance',
  q9WardenSupport: 'Warden Responsiveness',
  q10ComplaintResolution: 'Resolution Timeliness',
};

export const DgmSurveys: React.FC = () => {
  const [surveys, setSurveys] = useState<Survey[]>([]);
  const [selectedSurvey, setSelectedSurvey] = useState<Survey | null>(null);
  const [analytics, setAnalytics] = useState<SurveyAnalytics | null>(null);
  const [hostels, setHostels] = useState<Hostel[]>([]);
  const [wardens, setWardens] = useState<Warden[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingAnalytics, setIsLoadingAnalytics] = useState(false);

  // New Survey Modal
  const [isCreateSurveyOpen, setIsCreateSurveyOpen] = useState(false);
  const [surveyTitle, setSurveyTitle] = useState('');
  const [surveyDescription, setSurveyDescription] = useState('');
  const [targetHostelId, setTargetHostelId] = useState<string>('ALL');
  const [isSubmittingSurvey, setIsSubmittingSurvey] = useState(false);

  // Forward to Warden Modal
  const [isForwardModalOpen, setIsForwardModalOpen] = useState(false);
  const [forwardTitle, setForwardTitle] = useState('');
  const [forwardDescription, setForwardDescription] = useState('');
  const [forwardHostelId, setForwardHostelId] = useState<number>(1);
  const [forwardWardenId, setForwardWardenId] = useState<number>(1);
  const [forwardCategory, setForwardCategory] = useState('GENERAL');
  const [forwardPriority, setForwardPriority] = useState('HIGH');
  const [isSubmittingTask, setIsSubmittingTask] = useState(false);
  const [forwardSuccess, setForwardSuccess] = useState<string | null>(null);

  const fetchSurveys = async () => {
    setIsLoading(true);
    try {
      const [sList, hList, wList] = await Promise.all([
        apiService.getSurveys(),
        apiService.getHostels(),
        apiService.getWardens(),
      ]);
      setSurveys(sList);
      setHostels(hList);
      setWardens(wList);

      if (sList.length > 0 && !selectedSurvey) {
        setSelectedSurvey(sList[0]);
        loadAnalytics(sList[0].id);
      }
    } catch (err) {
      console.error('Failed to load surveys:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSurveys();
  }, []);

  const loadAnalytics = async (surveyId: number) => {
    setIsLoadingAnalytics(true);
    try {
      const data = await apiService.getSurveyAnalytics(surveyId);
      setAnalytics(data);
    } catch (err) {
      console.error('Failed to load analytics:', err);
    } finally {
      setIsLoadingAnalytics(false);
    }
  };

  const handleSelectSurvey = (s: Survey) => {
    setSelectedSurvey(s);
    loadAnalytics(s.id);
  };

  const handleCreateSurvey = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmittingSurvey(true);
    try {
      const created = await apiService.createSurvey({
        title: surveyTitle.trim(),
        description: surveyDescription.trim(),
        hostelId: targetHostelId === 'ALL' ? undefined : Number(targetHostelId),
      });
      setIsCreateSurveyOpen(false);
      setSurveyTitle('');
      setSurveyDescription('');
      await fetchSurveys();
      setSelectedSurvey(created);
      loadAnalytics(created.id);
    } catch (err) {
      console.error('Failed to create survey:', err);
    } finally {
      setIsSubmittingSurvey(false);
    }
  };

  const handleToggleSurveyStatus = async (s: Survey) => {
    try {
      await apiService.toggleSurveyStatus(s.id, !s.active);
      await fetchSurveys();
    } catch (err) {
      console.error('Failed to toggle status:', err);
    }
  };

  const openForwardModal = (prefillTitle: string, prefillDesc: string, category: string = 'MAINTENANCE') => {
    setForwardTitle(prefillTitle);
    setForwardDescription(prefillDesc);
    setForwardCategory(category);
    setForwardPriority('HIGH');
    if (hostels.length > 0) setForwardHostelId(hostels[0].id);
    if (wardens.length > 0) setForwardWardenId(wardens[0].id);
    setForwardSuccess(null);
    setIsForwardModalOpen(true);
  };

  const handleForwardTask = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmittingTask(true);
    try {
      await apiService.createFeedbackTask({
        title: forwardTitle.trim(),
        description: forwardDescription.trim(),
        surveyId: selectedSurvey?.id,
        hostelId: forwardHostelId,
        wardenId: forwardWardenId,
        category: forwardCategory,
        priority: forwardPriority,
      });
      setForwardSuccess('Action task successfully dispatched to warden dashboard!');
      setTimeout(() => {
        setIsForwardModalOpen(false);
        setForwardSuccess(null);
      }, 1500);
    } catch (err: any) {
      alert(err.message || 'Failed to dispatch task');
    } finally {
      setIsSubmittingTask(false);
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Student Feedback Surveys & Quality Analytics</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            10-Point institutional satisfaction benchmarks and warden accountability task dispatching
          </p>
        </div>

        <Button
          variant="primary"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => setIsCreateSurveyOpen(true)}
          className="text-xs"
        >
          Publish New Survey
        </Button>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Loading survey records...</div>
      ) : surveys.length === 0 ? (
        <EmptyState
          title="No surveys published"
          description="Click 'Publish New Survey' above to collect resident feedback across the 10 quality indicators."
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Survey List Sidebar */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              Survey Campaigns ({surveys.length})
            </h3>
            {surveys.map((s) => {
              const isSelected = selectedSurvey?.id === s.id;
              return (
                <Card
                  key={s.id}
                  onClick={() => handleSelectSurvey(s)}
                  className={`!p-4 cursor-pointer transition-all border ${
                    isSelected
                      ? 'border-blue-600 bg-blue-50/20 shadow-sm'
                      : 'hover:border-slate-300 bg-white'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2 mb-1">
                    <h4 className="text-xs font-bold text-slate-900 leading-tight">{s.title}</h4>
                    <span
                      className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                        s.active
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-slate-100 text-slate-500'
                      }`}
                    >
                      {s.active ? 'ACTIVE' : 'CLOSED'}
                    </span>
                  </div>

                  {s.description && (
                    <p className="text-[11px] text-slate-500 line-clamp-2 mb-2">{s.description}</p>
                  )}

                  <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-[11px] text-slate-400">
                    <span>Target: {s.targetHostelName || 'All Hostels'}</span>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleToggleSurveyStatus(s);
                      }}
                      className="text-slate-500 hover:text-blue-600 text-[10px] font-semibold cursor-pointer"
                    >
                      {s.active ? 'Close Survey' : 'Reopen'}
                    </button>
                  </div>
                </Card>
              );
            })}
          </div>

          {/* Analytics Dashboard */}
          <div className="lg:col-span-2 space-y-5">
            {isLoadingAnalytics ? (
              <div className="p-12 text-center text-slate-400 bg-white rounded-xl border border-slate-200">
                Loading satisfaction metrics...
              </div>
            ) : analytics ? (
              <>
                {/* Score Summary */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <Card className="!p-4 bg-gradient-to-br from-blue-600 to-indigo-700 text-white">
                    <span className="text-[11px] text-blue-100 font-semibold uppercase tracking-wider block mb-1">
                      Satisfaction Index
                    </span>
                    <div className="flex items-baseline gap-2">
                      <span className="text-3xl font-black">{analytics.overallSatisfactionScore}%</span>
                    </div>
                    <span className="text-[10px] text-blue-200 mt-1 block">
                      Target institutional threshold: 75%
                    </span>
                  </Card>

                  <Card className="!p-4">
                    <span className="text-[11px] text-slate-400 font-semibold uppercase tracking-wider block mb-1">
                      Resident Responses
                    </span>
                    <div className="flex items-baseline gap-2">
                      <span className="text-3xl font-black text-slate-900">{analytics.totalResponses}</span>
                      <span className="text-xs text-slate-500">students</span>
                    </div>
                    <span className="text-[10px] text-emerald-600 font-semibold mt-1 block">
                      Direct student feedback
                    </span>
                  </Card>

                  <Card className="!p-4">
                    <span className="text-[11px] text-slate-400 font-semibold uppercase tracking-wider block mb-1">
                      Action Dispatches
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      icon={<Send className="w-3.5 h-3.5 text-blue-600" />}
                      onClick={() =>
                        openForwardModal(
                          `Quality Improvement from ${selectedSurvey?.title}`,
                          `Review lowest rated amenities in this feedback survey and inspect physical facilities.`
                        )
                      }
                      className="mt-2 w-full text-xs"
                    >
                      Dispatch Warden Task
                    </Button>
                  </Card>
                </div>

                {/* 10-Question Ratings Breakdown */}
                <Card className="!p-5 space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                      <BarChart2 className="w-4 h-4 text-blue-600" />
                      10 Standard Institutional Quality Ratings (1 to 5 Scale)
                    </h4>
                    <span className="text-[11px] text-slate-400 font-mono">Scale: 1.0 (Low) - 5.0 (Max)</span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {[
                      { key: 'q1Cleanliness', label: '1. Cleanliness & Common Areas', val: analytics.q1CleanlinessAvg || 0 },
                      { key: 'q2Washrooms', label: '2. Washroom Hygiene', val: analytics.q2WashroomsAvg || 0 },
                      { key: 'q3Furniture', label: '3. Furniture & Bed Quality', val: analytics.q3FurnitureAvg || 0 },
                      { key: 'q4DrinkingWater', label: '4. RO Water Availability', val: analytics.q4DrinkingWaterAvg || 0 },
                      { key: 'q5Wifi', label: '5. Wi-Fi & Internet Speed', val: analytics.q5WifiAvg || 0 },
                      { key: 'q6Electricity', label: '6. Electricity & Power Backup', val: analytics.q6ElectricityAvg || 0 },
                      { key: 'q7Food', label: '7. Mess Food & Dining', val: analytics.q7FoodAvg || 0 },
                      { key: 'q8Security', label: '8. Security & Guard Vigilance', val: analytics.q8SecurityAvg || 0 },
                      { key: 'q9WardenSupport', label: '9. Warden Responsiveness', val: analytics.q9WardenSupportAvg || 0 },
                      { key: 'q10ComplaintResolution', label: '10. Complaint Speed', val: analytics.q10ComplaintResolutionAvg || 0 },
                    ].map((item) => {
                      const pct = Math.round((item.val / 5) * 100);
                      const isLow = item.val > 0 && item.val < 3.2;

                      return (
                        <div
                          key={item.key}
                          className={`p-3 rounded-lg border text-xs ${
                            isLow ? 'bg-red-50/50 border-red-200' : 'bg-slate-50 border-slate-100'
                          }`}
                        >
                          <div className="flex justify-between items-center mb-1">
                            <span className="font-semibold text-slate-800">{item.label}</span>
                            <span
                              className={`font-bold font-mono ${
                                isLow ? 'text-red-700' : 'text-slate-900'
                              }`}
                            >
                              {item.val.toFixed(1)} / 5.0
                            </span>
                          </div>
                          <div className="w-full bg-slate-200 rounded-full h-1.5 overflow-hidden">
                            <div
                              className={`h-full rounded-full ${
                                isLow ? 'bg-red-500' : 'bg-blue-600'
                              }`}
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                          {isLow && (
                            <div className="mt-2 flex justify-between items-center text-[10px]">
                              <span className="text-red-700 font-medium">Below Target Quality</span>
                              <button
                                onClick={() =>
                                  openForwardModal(
                                    `Priority Remedy: ${item.label}`,
                                    `Student survey results reported a low rating of ${item.val.toFixed(1)}/5.0 for ${item.label}. Immediate warden inspection and vendor repair required.`,
                                    'MAINTENANCE'
                                  )
                                }
                                className="text-red-700 font-bold hover:underline cursor-pointer"
                              >
                                Forward Task →
                              </button>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </Card>

                {/* Qualitative Comments */}
                <Card className="!p-5 space-y-3">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                    <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                      <MessageSquare className="w-4 h-4 text-slate-500" />
                      Resident Written Comments & Observations ({analytics.comments?.length || 0})
                    </h4>
                  </div>

                  {!analytics.comments || analytics.comments.length === 0 ? (
                    <p className="text-xs text-slate-400 italic py-4 text-center">
                      No written comments submitted for this survey.
                    </p>
                  ) : (
                    <div className="space-y-2.5">
                      {analytics.comments.map((c) => (
                        <div
                          key={c.id}
                          className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-2"
                        >
                          <div className="flex items-center justify-between text-[11px] text-slate-400">
                            <span className="font-semibold text-slate-700">{c.studentName}</span>
                            <span>{new Date(c.submittedAt).toLocaleDateString()}</span>
                          </div>
                          <p className="text-slate-800 font-medium italic">"{c.comments}"</p>
                          <div className="flex justify-end pt-1">
                            <button
                              onClick={() =>
                                openForwardModal(
                                  `Resident Grievance: ${c.studentName}`,
                                  `Resident reported the following observation during institutional survey: "${c.comments}". Investigate and report resolution.`,
                                  'GENERAL'
                                )
                              }
                              className="text-[11px] text-blue-600 font-semibold hover:underline flex items-center gap-1 cursor-pointer"
                            >
                              Forward as Action Task <ArrowRight className="w-3 h-3" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </Card>
              </>
            ) : null}
          </div>
        </div>
      )}

      {/* Create Survey Modal */}
      <Modal
        isOpen={isCreateSurveyOpen}
        onClose={() => setIsCreateSurveyOpen(false)}
        title="Publish Student Feedback Survey"
        size="md"
      >
        <form onSubmit={handleCreateSurvey} className="space-y-4">
          <Input
            label="Survey Title"
            value={surveyTitle}
            onChange={(e) => setSurveyTitle(e.target.value)}
            placeholder="e.g. Q4 Resident Quality Assessment"
            required
          />

          <Select
            label="Target Hostel Scope"
            value={targetHostelId}
            onChange={(e) => setTargetHostelId(e.target.value)}
            options={[
              { value: 'ALL', label: 'All Campus Hostels' },
              ...hostels.map((h) => ({ value: String(h.id), label: h.name })),
            ]}
          />

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Survey Context / Instructions
            </label>
            <textarea
              value={surveyDescription}
              onChange={(e) => setSurveyDescription(e.target.value)}
              rows={3}
              placeholder="State purpose of survey, evaluation timeline, and resident guidance..."
              className="w-full p-2.5 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsCreateSurveyOpen(false)}
              disabled={isSubmittingSurvey}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmittingSurvey}>
              Broadcast Survey
            </Button>
          </div>
        </form>
      </Modal>

      {/* Forward Action Task Modal */}
      <Modal
        isOpen={isForwardModalOpen}
        onClose={() => setIsForwardModalOpen(false)}
        title="Forward Feedback to Warden as Action Task"
        size="md"
      >
        <form onSubmit={handleForwardTask} className="space-y-4">
          {forwardSuccess ? (
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-lg text-xs text-emerald-800 text-center font-semibold">
              {forwardSuccess}
            </div>
          ) : (
            <>
              <Input
                label="Task Action Title"
                value={forwardTitle}
                onChange={(e) => setForwardTitle(e.target.value)}
                required
              />

              <div className="grid grid-cols-2 gap-3">
                <Select
                  label="Hostel"
                  value={String(forwardHostelId)}
                  onChange={(e) => setForwardHostelId(Number(e.target.value))}
                  options={hostels.map((h) => ({ value: String(h.id), label: h.name }))}
                />

                <Select
                  label="Assigned Warden"
                  value={String(forwardWardenId)}
                  onChange={(e) => setForwardWardenId(Number(e.target.value))}
                  options={wardens.map((w) => ({ value: String(w.id), label: w.name }))}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <Select
                  label="Category"
                  value={forwardCategory}
                  onChange={(e) => setForwardCategory(e.target.value)}
                  options={[
                    { value: 'GENERAL', label: 'General Quality' },
                    { value: 'MAINTENANCE', label: 'Facility Maintenance' },
                    { value: 'MESS', label: 'Food & Mess' },
                    { value: 'SECURITY', label: 'Campus Security' },
                  ]}
                />

                <Select
                  label="Priority"
                  value={forwardPriority}
                  onChange={(e) => setForwardPriority(e.target.value)}
                  options={[
                    { value: 'HIGH', label: 'High Priority' },
                    { value: 'MEDIUM', label: 'Medium Priority' },
                    { value: 'LOW', label: 'Low Priority' },
                  ]}
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Task Directives / Inspection Instructions
                </label>
                <textarea
                  value={forwardDescription}
                  onChange={(e) => setForwardDescription(e.target.value)}
                  rows={4}
                  className="w-full p-2.5 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500 font-sans"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsForwardModalOpen(false)}
                  disabled={isSubmittingTask}
                >
                  Cancel
                </Button>
                <Button type="submit" variant="primary" isLoading={isSubmittingTask}>
                  Assign Action Task
                </Button>
              </div>
            </>
          )}
        </form>
      </Modal>
    </div>
  );
};
